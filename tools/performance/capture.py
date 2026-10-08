"""Local Perfetto collection; no account cleanup, uploads or automated UI gestures."""
import argparse
import hashlib
import json
import re
import subprocess
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

PACKAGE = 'io.github.nook001.mywarwickplus.profile'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--mode', choices=['startup', 'manual'], default='startup')
    parser.add_argument('--samples', type=int, default=5)
    parser.add_argument('--duration', type=int, default=10)
    parser.add_argument('--label', default='baseline')
    parser.add_argument('--output', type=Path, default=Path('work/performance'))
    args = parser.parse_args()
    if not 5 <= args.duration <= 60 or not 1 <= args.samples <= 20:
        parser.error('duration must be 5..60 seconds; samples must be 1..20')
    if args.serial.startswith('emulator-') or not re.fullmatch(r'[A-Za-z0-9_-]+', args.label):
        parser.error('Use a physical serial and an alphanumeric label')
    if args.mode == 'manual' and args.samples != 1:
        parser.error('Manual collection requires --samples 1')

    def adb(*command, check=True, stdin=None):
        result = subprocess.run([args.adb, '-s', args.serial, *command],
                                capture_output=True, text=True, encoding='utf-8', errors='replace', input=stdin)
        if check and result.returncode:
            raise RuntimeError(f'ADB command failed: {command[0]} (exit {result.returncode}): '
                               + result.stderr.strip()[-600:])
        return result.stdout.strip()

    if adb('get-state') != 'device' or adb('shell', 'getprop', 'ro.kernel.qemu') == '1':
        raise RuntimeError('An online physical phone is required')
    package_info = adb('shell', 'dumpsys', 'package', PACKAGE)
    # Some OEM dumpsys versions omit profileable flags. The analyzer also requires
    # our profile-only trace markers; a non-debuggable APK alone is insufficient.
    if re.search(r'flags=\[.*DEBUGGABLE', package_info):
        raise RuntimeError('Install the non-debuggable profile APK before measuring')
    if 'mWakefulness=Awake' not in adb('shell', 'dumpsys', 'power'):
        raise RuntimeError('Keep the phone unlocked and screen on')
    version = re.search(r'versionName=(\S+)', package_info)
    run_id = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex[:8]
    output = args.output.resolve() / (args.label + '-' + run_id)
    output.mkdir(parents=True, exist_ok=False)
    installed_path = adb('shell', 'pm', 'path', PACKAGE).splitlines()[0]
    if not installed_path.startswith('package:/data/app/'):
        raise RuntimeError('Unexpected installed APK path')
    apk = output / 'installed.apk'
    adb('pull', installed_path.removeprefix('package:'), str(apk))
    config = f'''duration_ms: {args.duration * 1000}
buffers {{ size_kb: 65536 fill_policy: DISCARD }}
data_sources {{ config {{ name: "linux.ftrace" ftrace_config {{
  ftrace_events: "ftrace/print"
  ftrace_events: "sched/sched_switch"
  ftrace_events: "sched/sched_waking"
  ftrace_events: "power/cpu_frequency"
  atrace_categories: "am"
  atrace_categories: "wm"
  atrace_categories: "view"
  atrace_categories: "gfx"
  atrace_categories: "webview"
  atrace_categories: "dalvik"
  atrace_apps: "{PACKAGE}"
}} }} }}
data_sources {{ config {{ name: "linux.process_stats" process_stats_config {{
  scan_all_processes_on_start: true
  proc_stats_poll_ms: 1000
}} }} }}
data_sources {{ config {{ name: "android.surfaceflinger.frametimeline" }} }}
'''
    config_path = output / 'config.pbtxt'
    config_path.write_text(config, encoding='utf-8')
    metadata = dict(label=args.label, mode=args.mode, serial=args.serial, package=PACKAGE,
                    version=version.group(1) if version else None,
                    apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                    dexopt_status=re.findall(r'\[status=([^\]]+)\]', package_info),
                    sdk=adb('shell', 'getprop', 'ro.build.version.sdk'),
                    model=adb('shell', 'getprop', 'ro.product.model'),
                    duration_seconds=args.duration, compilation='unchanged', samples=[])
    metadata_path = output / 'capture.json'
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    try:
        for sample in range(1, args.samples + 1):
            remote_trace = f'/data/misc/perfetto-traces/mwp-{run_id}-{sample}.pftrace'
            trace_path = output / f'{sample:02}.pftrace'
            try:
                if args.mode == 'startup':
                    adb('shell', 'am', 'force-stop', PACKAGE)
                ready = adb('shell', 'perfetto', '--background-wait', '--txt',
                            '-c', '-', '-o', remote_trace, stdin=config)
                pids = re.findall(r'^\d+$', ready, flags=re.M)
                if not pids:
                    raise RuntimeError('Perfetto did not acknowledge a recorder PID')
                started = time.monotonic()
                print(f'Capture {sample}/{args.samples}: {args.mode} ({args.duration}s)', flush=True)
                launch = ''
                if args.mode == 'startup':
                    launch = adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/uk.ac.warwick.plus.MainActivity')
                    if 'Status: ok' not in launch:
                        raise RuntimeError('Cold launch failed')
                else:
                    print('Use the phone manually now; no gestures are generated.', flush=True)
                time.sleep(max(0, args.duration + 1 - (time.monotonic() - started)))
                adb('pull', remote_trace, str(trace_path))
                if trace_path.stat().st_size == 0:
                    raise RuntimeError('Trace is empty')
                total = re.search(r'TotalTime:\s*(\d+)', launch)
                metadata['samples'].append(dict(file=trace_path.name,
                    am_total_ms=int(total.group(1)) if total else None,
                    bytes=trace_path.stat().st_size))
                metadata_path.write_text(json.dumps(metadata, indent=2), encoding='utf-8')
                print(f'Saved {trace_path.name}', flush=True)
            finally:
                # The recorder has its own bounded duration, even on failure.
                adb('shell', 'rm', '-f', remote_trace, check=False)
            if sample < args.samples:
                time.sleep(3)
    finally:
        metadata['complete'] = len(metadata['samples']) == args.samples
        metadata_path.write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    print(f'Artifacts: {output}', flush=True)


if __name__ == '__main__':
    main()
