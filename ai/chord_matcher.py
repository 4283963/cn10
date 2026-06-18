import numpy as np
import json
import sys
import os
import signal

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model.chord_nn import ChordNeuralNetwork

NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
NOTE_TO_INDEX = {note: i for i, note in enumerate(NOTE_NAMES)}

CHORD_TYPES = ['maj', 'min', 'dim', 'aug']

COMMON_CHORDS = [
    'C', 'Cm', 'Cdim', 'Caug',
    'C#', 'C#m', 'C#dim', 'C#aug',
    'D', 'Dm', 'Ddim', 'Daug',
    'D#', 'D#m', 'D#dim', 'D#aug',
    'E', 'Em', 'Edim', 'Eaug',
    'F', 'Fm', 'Fdim', 'Faug',
    'F#', 'F#m', 'F#dim', 'F#aug',
    'G', 'Gm', 'Gdim', 'Gaug',
    'G#', 'G#m', 'G#dim', 'G#aug',
    'A', 'Am', 'Adim', 'Aaug',
    'A#', 'A#m', 'A#dim', 'A#aug',
    'B', 'Bm', 'Bdim', 'Baug'
]

CHORD_NOTES = {
    'maj': [0, 4, 7],
    'min': [0, 3, 7],
    'dim': [0, 3, 6],
    'aug': [0, 4, 8],
    'maj7': [0, 4, 7, 11],
    'min7': [0, 3, 7, 10],
    'dom7': [0, 4, 7, 10],
}

CHORD_PROGRESSION_SCORES = {
    ('C', 'F'): 0.8, ('C', 'G'): 0.9, ('C', 'Am'): 0.85,
    ('Am', 'F'): 0.8, ('Am', 'G'): 0.7, ('Am', 'C'): 0.85,
    ('F', 'C'): 0.9, ('F', 'G'): 0.85, ('F', 'Am'): 0.75,
    ('G', 'C'): 0.95, ('G', 'Am'): 0.7, ('G', 'F'): 0.6,
    ('Dm', 'G'): 0.9, ('Dm', 'Am'): 0.8,
    ('Em', 'Am'): 0.9, ('Em', 'C'): 0.7,
}

STYLE_CONFIGS = {
    'folk': {
        'name': '民谣风格',
        'chord_complexity': 'simple',
        'prefer_open_chords': True,
        'rhythm_type': 'strumming',
        'tempo_modifier': 0.85,
        'chord_bonus': ['maj', 'min'],
        'description': '温暖的木吉他扫弦，简单优美的三和弦进行'
    },
    'rock': {
        'name': '摇滚风格',
        'chord_complexity': 'power',
        'prefer_open_chords': False,
        'rhythm_type': 'power_chord',
        'tempo_modifier': 1.2,
        'chord_bonus': ['maj', 'power', 'dom7'],
        'description': '充满力量的五和弦与强力扫弦，节奏感强烈'
    },
    'classical': {
        'name': '古典风格',
        'chord_complexity': 'rich',
        'prefer_open_chords': False,
        'rhythm_type': 'arpeggio',
        'tempo_modifier': 0.9,
        'chord_bonus': ['maj7', 'min7', 'dim', 'maj', 'min'],
        'description': '精致的琶音分解和弦，丰富的七和弦与色彩和弦'
    }
}

RHYTHM_PATTERNS = {
    'folk': {
        'strumming': {
            'pattern_name': '下上下上 民谣扫弦',
            'strokes_per_measure': 8,
            'strokes': [
                {'time': 0.0, 'direction': 'down', 'emphasis': 1.0},
                {'time': 0.5, 'direction': 'up', 'emphasis': 0.6},
                {'time': 1.0, 'direction': 'down', 'emphasis': 0.8},
                {'time': 1.5, 'direction': 'up', 'emphasis': 0.5},
                {'time': 2.0, 'direction': 'down', 'emphasis': 1.0},
                {'time': 2.5, 'direction': 'up', 'emphasis': 0.6},
                {'time': 3.0, 'direction': 'down', 'emphasis': 0.8},
                {'time': 3.5, 'direction': 'up', 'emphasis': 0.7},
            ],
            'bass_note_beat': [0, 2]
        }
    },
    'rock': {
        'power_chord': {
            'pattern_name': '八分音符 强力和弦',
            'strokes_per_measure': 8,
            'strokes': [
                {'time': 0.0, 'direction': 'down', 'emphasis': 1.0},
                {'time': 0.5, 'direction': 'down', 'emphasis': 0.7},
                {'time': 1.0, 'direction': 'down', 'emphasis': 0.9},
                {'time': 1.5, 'direction': 'down', 'emphasis': 0.6},
                {'time': 2.0, 'direction': 'down', 'emphasis': 1.0},
                {'time': 2.5, 'direction': 'down', 'emphasis': 0.7},
                {'time': 3.0, 'direction': 'down', 'emphasis': 0.9},
                {'time': 3.5, 'direction': 'down', 'emphasis': 0.8},
            ],
            'bass_note_beat': [0, 1, 2, 3]
        }
    },
    'classical': {
        'arpeggio': {
            'pattern_name': '琶音分解 古典风格',
            'strokes_per_measure': 12,
            'strokes': [
                {'time': 0.0, 'string': 'bass', 'emphasis': 1.0},
                {'time': 0.333, 'string': 'high', 'emphasis': 0.7},
                {'time': 0.666, 'string': 'mid', 'emphasis': 0.6},
                {'time': 1.0, 'string': 'low', 'emphasis': 0.8},
                {'time': 1.333, 'string': 'mid', 'emphasis': 0.7},
                {'time': 1.666, 'string': 'high', 'emphasis': 0.6},
                {'time': 2.0, 'string': 'bass', 'emphasis': 1.0},
                {'time': 2.333, 'string': 'high', 'emphasis': 0.7},
                {'time': 2.666, 'string': 'mid', 'emphasis': 0.6},
                {'time': 3.0, 'string': 'low', 'emphasis': 0.8},
                {'time': 3.333, 'string': 'mid', 'emphasis': 0.7},
                {'time': 3.666, 'string': 'high', 'emphasis': 0.6},
            ],
            'bass_note_beat': [0, 2]
        }
    }
}


def _signal_handler(signum, frame):
    sys.exit(128 + signum)


signal.signal(signal.SIGTERM, _signal_handler)
signal.signal(signal.SIGINT, _signal_handler)


def note_to_pitch_class(note_name):
    note = note_name.upper().replace('B', 'B')
    if '#' in note:
        base = note[0]
        idx = NOTE_TO_INDEX.get(base, 0)
        return (idx + 1) % 12
    elif note.endswith('B') and len(note) > 1:
        base = note[0]
        idx = NOTE_TO_INDEX.get(base, 0)
        return (idx - 1) % 12
    return NOTE_TO_INDEX.get(note, 0)


def get_chord_notes(chord_name):
    root = chord_name[0]
    rest = chord_name[1:]
    if rest.startswith('#') or rest.startswith('b'):
        root = chord_name[:2]
        rest = chord_name[2:]

    root_idx = note_to_pitch_class(root)

    if rest.startswith('maj7'):
        chord_type = 'maj7'
    elif rest.startswith('min7') or rest.startswith('m7'):
        chord_type = 'min7'
    elif rest.startswith('dim'):
        chord_type = 'dim'
    elif rest.startswith('aug'):
        chord_type = 'aug'
    elif rest.startswith('maj') or rest == '':
        chord_type = 'maj'
    elif rest.startswith('min') or rest.startswith('m'):
        chord_type = 'min'
    elif rest.startswith('7'):
        chord_type = 'dom7'
    else:
        chord_type = 'maj'

    intervals = CHORD_NOTES.get(chord_type, [0, 4, 7])
    return [(root_idx + interval) % 12 for interval in intervals]


def parse_input(input_data):
    try:
        data = json.loads(input_data)
        if isinstance(data, dict):
            notes = data.get('notes', [])
            style = data.get('style', 'folk')
            return notes, style
        else:
            return data, 'folk'
    except json.JSONDecodeError:
        try:
            notes = json.loads(input_data)
            return notes, 'folk'
        except Exception:
            return [], 'folk'


def parse_notes(notes_list):
    parsed = []
    for note in notes_list:
        pitch = note.get('pitch', note.get('note', ''))
        duration = note.get('duration', note.get('beat', 1.0))
        parsed.append({
            'pitch': pitch,
            'pitch_class': note_to_pitch_class(pitch),
            'duration': float(duration)
        })
    return parsed


def split_into_measures(notes, beats_per_measure=4.0):
    measures = []
    current_measure = []
    current_duration = 0.0

    for note in notes:
        if current_duration + note['duration'] > beats_per_measure and current_measure:
            measures.append(current_measure)
            current_measure = []
            current_duration = 0.0
        current_measure.append(note)
        current_duration += note['duration']

    if current_measure:
        measures.append(current_measure)

    return measures


def measure_note_distribution(measure):
    total_duration = sum(note['duration'] for note in measure)
    if total_duration == 0:
        total_duration = 1.0

    distribution = np.zeros(12)
    for note in measure:
        weight = note['duration'] / total_duration
        distribution[note['pitch_class']] += weight

    return distribution


def chord_note_match_score(chord_name, note_distribution):
    chord_pcs = get_chord_notes(chord_name)
    score = 0.0
    for pc in chord_pcs:
        score += note_distribution[pc]
    return score / len(chord_pcs)


def root_note_score(chord_name, note_distribution):
    root = chord_name[0]
    if len(chord_name) > 1 and (chord_name[1] == '#' or chord_name[1] == 'b'):
        root = chord_name[:2]
    root_pc = note_to_pitch_class(root)
    return note_distribution[root_pc] * 0.5


def chord_progression_score(prev_chord, curr_chord):
    if prev_chord is None:
        return 0.0
    return CHORD_PROGRESSION_SCORES.get((prev_chord, curr_chord), 0.3)


def generate_candidate_chords(style='folk'):
    candidates = []
    base_notes = ['C', 'D', 'E', 'F', 'G', 'A', 'B']

    if style == 'rock':
        for note in base_notes:
            candidates.append(note)
            candidates.append(note + 'm')
        return candidates

    if style == 'classical':
        for note in base_notes:
            candidates.append(note)
            candidates.append(note + 'm')
            candidates.append(note + '7')
            candidates.append(note + 'maj7')
            candidates.append(note + 'm7')
            candidates.append(note + 'dim')
        for note in ['C#', 'F#']:
            candidates.append(note)
            candidates.append(note + 'm')
        return candidates

    for note in base_notes:
        candidates.append(note)
        candidates.append(note + 'm')
    for note in ['C#', 'F#']:
        candidates.append(note)
        candidates.append(note + 'm')
    return candidates


def build_input_vector(note_distribution, prev_chord_idx=-1):
    vec = np.zeros(24)
    vec[:12] = note_distribution
    if prev_chord_idx >= 0 and prev_chord_idx < 12:
        vec[12 + prev_chord_idx] = 1.0
    return vec


def determine_key(notes):
    distribution = np.zeros(12)
    total_duration = 0.0
    for note in notes:
        distribution[note['pitch_class']] += note['duration']
        total_duration += note['duration']

    if total_duration > 0:
        distribution /= total_duration

    major_keys = []
    for i in range(12):
        major_profile = np.array([1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0])
        shifted = np.roll(major_profile, i)
        score = np.sum(distribution * shifted)
        major_keys.append((NOTE_NAMES[i], score, 'major'))

    minor_keys = []
    for i in range(12):
        minor_profile = np.array([1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0])
        shifted = np.roll(minor_profile, i)
        score = np.sum(distribution * shifted)
        minor_keys.append((NOTE_NAMES[i], score, 'minor'))

    all_keys = major_keys + minor_keys
    all_keys.sort(key=lambda x: x[1], reverse=True)

    return all_keys[0]


def get_diatonic_chords(key_note, key_type):
    root_idx = note_to_pitch_class(key_note)

    if key_type == 'major':
        scale_degrees = [0, 2, 4, 5, 7, 9, 11]
        chord_qualities = ['maj', 'min', 'min', 'maj', 'maj', 'min', 'dim']
    else:
        scale_degrees = [0, 2, 3, 5, 7, 8, 10]
        chord_qualities = ['min', 'dim', 'maj', 'min', 'min', 'maj', 'maj']

    chords = []
    for i, degree in enumerate(scale_degrees):
        pc = (root_idx + degree) % 12
        name = NOTE_NAMES[pc]
        quality = chord_qualities[i]
        if quality == 'maj':
            chords.append(name)
        elif quality == 'min':
            chords.append(name + 'm')
        elif quality == 'dim':
            chords.append(name + 'dim')
        else:
            chords.append(name + quality)

    return chords


def style_chord_bonus(chord, style):
    if style == 'folk':
        if chord in ['C', 'G', 'D', 'Em', 'Am', 'F']:
            return 0.12
        if chord.endswith('7') or 'dim' in chord or 'aug' in chord:
            return -0.1
        return 0.0

    if style == 'rock':
        if chord in ['C', 'G', 'D', 'A', 'E']:
            return 0.1
        if chord.endswith('m'):
            return 0.05
        if '7' in chord:
            return 0.08
        return 0.0

    if style == 'classical':
        if 'maj7' in chord or 'm7' in chord:
            return 0.15
        if 'dim' in chord:
            return 0.05
        if '7' in chord:
            return 0.08
        return 0.0

    return 0.0


def generate_accompaniment_track(chords, style, beats_per_measure=4.0):
    style_config = STYLE_CONFIGS.get(style, STYLE_CONFIGS['folk'])
    rhythm_type = style_config['rhythm_type']
    pattern = RHYTHM_PATTERNS.get(style, {}).get(rhythm_type, {})

    accompaniment = []
    for measure_idx, chord in enumerate(chords):
        measure_start = measure_idx * beats_per_measure
        chord_notes = get_chord_notes(chord)

        if not chord_notes:
            continue

        if style == 'classical':
            for stroke in pattern.get('strokes', []):
                note_idx = 0
                string = stroke.get('string', 'mid')
                if string == 'bass':
                    note_idx = 0
                elif string == 'low':
                    note_idx = 0
                elif string == 'mid':
                    note_idx = 1 % len(chord_notes)
                elif string == 'high':
                    note_idx = (len(chord_notes) - 1) % len(chord_notes)

                pc = chord_notes[note_idx % len(chord_notes)]
                accompaniment.append({
                    'measure': measure_idx,
                    'time': measure_start + stroke['time'],
                    'pitch_class': pc,
                    'pitch': NOTE_NAMES[pc],
                    'velocity': stroke.get('emphasis', 0.7),
                    'duration': 0.3,
                    'type': 'arpeggio'
                })
        elif style == 'rock':
            for stroke in pattern.get('strokes', []):
                for i, pc in enumerate(chord_notes[:3]):
                    accompaniment.append({
                        'measure': measure_idx,
                        'time': measure_start + stroke['time'],
                        'pitch_class': pc,
                        'pitch': NOTE_NAMES[pc],
                        'velocity': stroke.get('emphasis', 0.8) * (0.9 if i > 0 else 1.0),
                        'duration': 0.4,
                        'type': 'power_chord'
                    })
        else:
            for stroke in pattern.get('strokes', []):
                for i, pc in enumerate(chord_notes):
                    accompaniment.append({
                        'measure': measure_idx,
                        'time': measure_start + stroke['time'],
                        'pitch_class': pc,
                        'pitch': NOTE_NAMES[pc],
                        'velocity': stroke.get('emphasis', 0.7),
                        'duration': 0.35,
                        'type': 'strum'
                    })

    return accompaniment


def match_chords(notes_list, style='folk', model_path=None):
    notes = parse_notes(notes_list)

    if not notes:
        return {
            'chords': [],
            'key': 'C',
            'key_type': 'major',
            'style': style,
            'style_info': STYLE_CONFIGS.get(style, STYLE_CONFIGS['folk']),
            'rhythm_pattern': {},
            'accompaniment': [],
            'num_measures': 0,
            'confidence': 0.0
        }

    key_note, key_score, key_type = determine_key(notes)
    diatonic_chords = get_diatonic_chords(key_note, key_type)

    measures = split_into_measures(notes)

    model = ChordNeuralNetwork()
    model_loaded = False
    if model_path and os.path.exists(model_path):
        model.load_model(model_path)
        model_loaded = True

    result_chords = []
    prev_chord = None

    for measure in measures:
        note_dist = measure_note_distribution(measure)

        candidates = generate_candidate_chords(style)
        candidates = list(set(candidates))

        scores = {}
        for chord in candidates:
            match_score = chord_note_match_score(chord, note_dist)
            root_score = root_note_score(chord, note_dist)
            prog_score = chord_progression_score(prev_chord, chord)
            diatonic_bonus = 0.15 if chord in diatonic_chords else 0.0
            style_bonus = style_chord_bonus(chord, style)

            total_score = (match_score * 0.4 +
                           root_score * 0.2 +
                           prog_score * 0.25 +
                           diatonic_bonus +
                           style_bonus)

            if model_loaded:
                prev_idx = -1
                if prev_chord and prev_chord in NOTE_NAMES:
                    prev_idx = note_to_pitch_class(prev_chord[0])
                input_vec = build_input_vector(note_dist, prev_idx)
                nn_output = model.forward(input_vec)
                chord_idx = note_to_pitch_class(chord[0])
                nn_score = nn_output[chord_idx % 24]
                total_score = total_score * 0.7 + nn_score * 0.3

            scores[chord] = total_score

        best_chord = max(scores, key=scores.get)
        result_chords.append(best_chord)
        prev_chord = best_chord

    style_config = STYLE_CONFIGS.get(style, STYLE_CONFIGS['folk'])
    rhythm_type = style_config['rhythm_type']
    rhythm_pattern = RHYTHM_PATTERNS.get(style, {}).get(rhythm_type, {})

    accompaniment = generate_accompaniment_track(result_chords, style)

    result = {
        'chords': result_chords,
        'key': key_note,
        'key_type': key_type,
        'style': style,
        'style_info': style_config,
        'rhythm_pattern': rhythm_pattern,
        'accompaniment': accompaniment,
        'num_measures': len(measures),
        'confidence': float(key_score)
    }

    return result


def _read_input():
    if not sys.stdin.isatty():
        data = sys.stdin.read()
        if data and data.strip():
            return data.strip()

    if len(sys.argv) >= 2:
        return sys.argv[1]

    return None


if __name__ == '__main__':
    input_data = _read_input()

    if input_data is None:
        print(json.dumps({'error': 'No input provided. Pass notes JSON via stdin or command line argument.'}))
        sys.exit(1)

    try:
        notes_list, style = parse_input(input_data)
        if style not in STYLE_CONFIGS:
            style = 'folk'

        result = match_chords(notes_list, style)
        output = json.dumps(result, ensure_ascii=False)
        sys.stdout.write(output)
        sys.stdout.write('\n')
        sys.stdout.flush()
        sys.exit(0)
    except Exception as e:
        try:
            error_output = json.dumps({'error': str(e)})
            sys.stderr.write(error_output)
            sys.stderr.write('\n')
            sys.stderr.flush()
        except Exception:
            pass
        sys.exit(1)
