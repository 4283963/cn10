import numpy as np
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model.chord_nn import ChordNeuralNetwork

NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
NOTE_TO_INDEX = {note: i for i, note in enumerate(NOTE_NAMES)}


def note_to_pitch_class(note_name):
    note = note_name.upper()
    return NOTE_TO_INDEX.get(note, 0)


def generate_training_data():
    X = []
    y = []

    chord_progressions = [
        {'key': 'C', 'type': 'major', 'chords': ['C', 'G', 'Am', 'F'],
         'melodies': [
             [('C', 1), ('D', 1), ('E', 1), ('C', 1)],
             [('G', 1), ('A', 1), ('B', 1), ('G', 1)],
             [('A', 1), ('C', 1), ('E', 1), ('A', 1)],
             [('F', 1), ('A', 1), ('C', 1), ('F', 1)],
         ]},
        {'key': 'C', 'type': 'major', 'chords': ['C', 'Am', 'F', 'G'],
         'melodies': [
             [('C', 1), ('E', 1), ('G', 1), ('C', 1)],
             [('A', 1), ('C', 1), ('E', 1), ('A', 1)],
             [('F', 1), ('A', 1), ('C', 1), ('F', 1)],
             [('G', 1), ('B', 1), ('D', 1), ('G', 1)],
         ]},
        {'key': 'G', 'type': 'major', 'chords': ['G', 'C', 'D', 'Em'],
         'melodies': [
             [('G', 1), ('B', 1), ('D', 1), ('G', 1)],
             [('C', 1), ('E', 1), ('G', 1), ('C', 1)],
             [('D', 1), ('F#', 1), ('A', 1), ('D', 1)],
             [('E', 1), ('G', 1), ('B', 1), ('E', 1)],
         ]},
        {'key': 'Am', 'type': 'minor', 'chords': ['Am', 'F', 'C', 'G'],
         'melodies': [
             [('A', 1), ('C', 1), ('E', 1), ('A', 1)],
             [('F', 1), ('A', 1), ('C', 1), ('F', 1)],
             [('C', 1), ('E', 1), ('G', 1), ('C', 1)],
             [('G', 1), ('B', 1), ('D', 1), ('G', 1)],
         ]},
        {'key': 'Dm', 'type': 'minor', 'chords': ['Dm', 'Am', 'Bb', 'C'],
         'melodies': [
             [('D', 1), ('F', 1), ('A', 1), ('D', 1)],
             [('A', 1), ('C', 1), ('E', 1), ('A', 1)],
             [('A#', 1), ('D', 1), ('F', 1), ('A#', 1)],
             [('C', 1), ('E', 1), ('G', 1), ('C', 1)],
         ]},
    ]

    for prog in chord_progressions:
        for i, chord in enumerate(prog['chords']):
            melody = prog['melodies'][i]
            distribution = np.zeros(12)
            total_dur = 0

            for note, dur in melody:
                pc = note_to_pitch_class(note)
                distribution[pc] += dur
                total_dur += dur

            if total_dur > 0:
                distribution /= total_dur

            prev_idx = -1
            if i > 0:
                prev_chord = prog['chords'][i - 1]
                prev_idx = note_to_pitch_class(prev_chord[0])

            input_vec = np.zeros(24)
            input_vec[:12] = distribution
            if prev_idx >= 0:
                input_vec[12 + prev_idx] = 1.0

            output_vec = np.zeros(24)
            chord_root_idx = note_to_pitch_class(chord[0])
            output_vec[chord_root_idx % 24] = 1.0

            X.append(input_vec)
            y.append(output_vec)

            for _ in range(5):
                noisy_input = input_vec + np.random.normal(0, 0.05, 24)
                noisy_input = np.clip(noisy_input, 0, 1)
                X.append(noisy_input)
                y.append(output_vec)

    return np.array(X), np.array(y)


def main():
    model_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'model')
    model_path = os.path.join(model_dir, 'chord_model.npz')

    print("Generating training data...")
    X, y = generate_training_data()
    print(f"Training samples: {len(X)}")

    print("Initializing neural network...")
    model = ChordNeuralNetwork()

    print("Training model...")
    model.train(X, y, epochs=500, learning_rate=0.05)

    print(f"Saving model to {model_path}...")
    model.save_model(model_path)

    print("Training complete!")

    test_input = np.zeros(24)
    test_input[0] = 0.5
    test_input[4] = 0.3
    test_input[7] = 0.2
    output = model.forward(test_input)
    top_idx = np.argmax(output)
    print(f"\nTest prediction: C major melody -> {NOTE_NAMES[top_idx % 12]} (confidence: {output[top_idx]:.3f})")


if __name__ == '__main__':
    main()
