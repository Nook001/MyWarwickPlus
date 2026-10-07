"""Summarize local app-only timings. Missing markers or trace errors reject a sample."""
import argparse
import csv
import io
import json
import statistics
import subprocess
from pathlib import Path

PACKAGE = 'uk.ac.warwick.plus'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('capture', type=Path)
    parser.add_argument('--processor', required=True, type=Path)
    args = parser.parse_args()
    metadata = json.loads((args.capture / 'capture.json').read_text(encoding='utf-8'))
    if not metadata.get('complete'):
        raise RuntimeError('Capture is incomplete')

    def query(trace, sql):
        result = subprocess.run([str(args.processor), 'query', str(trace), sql],
                                capture_output=True, text=True, encoding='utf-8', errors='replace')
        if result.returncode:
            raise RuntimeError(f'Trace query failed for {trace.name}: {result.stderr[-500:]}')
        return list(csv.DictReader(io.StringIO(result.stdout)))

    samples = []
    app_process_filter = f"""(p.name = '{PACKAGE}' OR p.upid IN (
        SELECT DISTINCT t.upid FROM slice m JOIN thread_track tr ON tr.id = m.track_id
        JOIN thread t USING(utid) WHERE m.name GLOB 'MWP.*'))"""
    for sample in metadata['samples']:
        trace = args.capture / sample['file']
        quality = query(trace, """SELECT name, value FROM stats WHERE value > 0 AND
            (severity = 'error' OR name GLOB '*overrun*' OR name GLOB '*discard*'
             OR name GLOB '*overwrite*')
            AND name NOT IN ('traced_chunks_discarded', 'traced_patches_discarded')""")
        # These service-wide counters are cumulative across sessions, unlike
        # traced_buf_* loss counters. Preserve them without treating prior loss
        # in unrelated/finished sessions as corruption of this trace.
        service_counters = query(trace, """SELECT name, value FROM stats WHERE value > 0
            AND name IN ('traced_chunks_discarded', 'traced_patches_discarded')""")
        sections = query(trace, f"""SELECT s.name, t.is_main_thread,
            ROUND(s.dur / 1e6, 3) AS ms FROM slice s
            JOIN thread_track tt ON tt.id = s.track_id
            JOIN thread t ON t.utid = tt.utid
            WHERE s.name GLOB 'MWP.*' AND s.dur >= 0
            ORDER BY s.ts""")
        if quality or (metadata['mode'] == 'startup' and not sections):
            raise RuntimeError(f'Rejected {trace.name}: missing profile markers or {quality}')
        startup = query(trace, f"""INCLUDE PERFETTO MODULE android.startup.time_to_display;
            WITH app_processes AS (
              SELECT DISTINCT t.upid FROM slice m JOIN thread_track tr ON tr.id = m.track_id
              JOIN thread t USING(utid) WHERE m.name GLOB 'MWP.*'
            ), first_frames AS (
              SELECT s.startup_id, d.ts + d.dur - s.ts AS ttid,
                ROW_NUMBER() OVER (PARTITION BY s.startup_id ORDER BY f.ts) AS n
              FROM android_startups s JOIN android_frames f
                ON f.upid IN (SELECT upid FROM app_processes) AND f.ts >= s.ts
              JOIN slice d ON d.id = f.draw_frame_id
              WHERE s.package = '{PACKAGE}' AND d.dur >= 0
            )
            SELECT s.startup_type, ROUND(s.dur / 1e6, 3) AS startup_ms,
              ROUND(COALESCE(t.time_to_initial_display, f.ttid) / 1e6, 3) AS ttid_ms,
              CASE WHEN t.time_to_initial_display IS NULL THEN 'app_marker_first_frame'
                ELSE 'perfetto_startup_module' END AS ttid_source
            FROM android_startups s LEFT JOIN android_startup_time_to_display t USING(startup_id)
            LEFT JOIN first_frames f ON f.startup_id = s.startup_id AND f.n = 1
            WHERE s.package = '{PACKAGE}'""")
        if metadata['mode'] == 'startup' and (len(startup) != 1 or
                startup[0]['startup_type'] != 'cold' or startup[0]['ttid_ms'] in ('[NULL]', '')):
            raise RuntimeError(f'Rejected {trace.name}: missing cold-start TTID')
        frames = query(trace, f"""SELECT COUNT(*) AS frames,
            SUM(CASE WHEN jank_type NOT IN ('None', '') THEN 1 ELSE 0 END) AS janky_frames,
            ROUND(AVG(dur) / 1e6, 3) AS mean_frame_ms
            FROM actual_frame_timeline_slice f JOIN process p USING(upid)
            WHERE {app_process_filter} AND dur >= 0""")
        frame_types = query(trace, f"""SELECT jank_type, present_type, COUNT(*) AS frames,
            ROUND(AVG(dur)/1e6,3) AS mean_frame_ms
            FROM actual_frame_timeline_slice f JOIN process p USING(upid)
            WHERE {app_process_filter} AND dur >= 0 GROUP BY jank_type,present_type""")
        if metadata['mode'] == 'manual' and int(frames[0]['frames']) == 0:
            raise RuntimeError(f'Rejected {trace.name}: no app frame timeline')
        samples.append(dict(file=sample['file'], am_total_ms=sample['am_total_ms'],
                            startup=startup, sections=sections, frames=frames, frame_types=frame_types,
                            trace_errors=quality,
                            service_counters=service_counters))

    summary = dict(label=metadata['label'], version=metadata['version'], mode=metadata['mode'],
                   sdk=metadata['sdk'], samples=len(samples), first_sections_ms={})
    if metadata['mode'] == 'startup':
        ttid = [float(item['startup'][0]['ttid_ms']) for item in samples]
        summary['ttid_ms'] = dict(median=round(statistics.median(ttid), 3), min=min(ttid), max=max(ttid), values=ttid)
    names = sorted({row['name'] for item in samples for row in item['sections']})
    for name in names:
        first = [next((row for row in item['sections'] if row['name'] == name), None) for item in samples]
        rows = [row for row in first if row is not None]
        durations = [float(row['ms']) for row in rows]
        summary['first_sections_ms'][name] = dict(samples=len(rows),
            median=round(statistics.median(durations), 3), min=min(durations), max=max(durations),
            main_thread_samples=sum(row['is_main_thread'] == '1' for row in rows))
    report = dict(summary=summary, measurements=samples,
        caveats=['Process cold start; OS page cache and ART compilation are unchanged.',
                 'Online refresh and OEM/system scheduling are not controlled.',
                 'Startup frame counts are not a scrolling benchmark.',
                 'Timings do not establish Baseline Profile benefit or user acceptance.'])
    (args.capture / 'analysis.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
