import numpy as np
import json
import sys
import os

class ChordNeuralNetwork:
    def __init__(self):
        self.input_size = 24
        self.hidden_size = 32
        self.output_size = 24
        self.weights1 = None
        self.weights2 = None
        self.bias1 = None
        self.bias2 = None
        self._initialize_weights()

    def _initialize_weights(self):
        np.random.seed(42)
        self.weights1 = np.random.randn(self.input_size, self.hidden_size) * 0.1
        self.weights2 = np.random.randn(self.hidden_size, self.output_size) * 0.1
        self.bias1 = np.zeros(self.hidden_size)
        self.bias2 = np.zeros(self.output_size)

    def sigmoid(self, x):
        return 1 / (1 + np.exp(-np.clip(x, -500, 500)))

    def softmax(self, x):
        exp_x = np.exp(x - np.max(x))
        return exp_x / np.sum(exp_x)

    def forward(self, x):
        z1 = np.dot(x, self.weights1) + self.bias1
        a1 = self.sigmoid(z1)
        z2 = np.dot(a1, self.weights2) + self.bias2
        a2 = self.softmax(z2)
        return a2

    def train(self, X, y, epochs=1000, learning_rate=0.01):
        for epoch in range(epochs):
            for i in range(len(X)):
                z1 = np.dot(X[i], self.weights1) + self.bias1
                a1 = self.sigmoid(z1)
                z2 = np.dot(a1, self.weights2) + self.bias2
                a2 = self.softmax(z2)

                delta2 = a2 - y[i]
                delta1 = np.dot(delta2, self.weights2.T) * a1 * (1 - a1)

                self.weights2 -= learning_rate * np.outer(a1, delta2)
                self.bias2 -= learning_rate * delta2
                self.weights1 -= learning_rate * np.outer(X[i], delta1)
                self.bias1 -= learning_rate * delta1

    def save_model(self, path):
        np.savez(path,
                 weights1=self.weights1,
                 weights2=self.weights2,
                 bias1=self.bias1,
                 bias2=self.bias2)

    def load_model(self, path):
        if os.path.exists(path):
            data = np.load(path)
            self.weights1 = data['weights1']
            self.weights2 = data['weights2']
            self.bias1 = data['bias1']
            self.bias2 = data['bias2']
            return True
        return False
