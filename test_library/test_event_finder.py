import pytest
import numpy as np
import json

from event_finder import *


def test_group_indexes_single_range():
    indexes = [1, 2, 3, 4]
    result = list(group_indexes(indexes))
    assert result == [(1, 5)]

def test_group_indexes_multiple_ranges():
    indexes = [1, 2, 3, 5, 6, 8]
    result = list(group_indexes(indexes))
    assert result == [(1, 4), (5, 7), (8, 9)]

def test_group_indexes_non_consecutive():
    indexes = [1, 3, 5, 7]
    result = list(group_indexes(indexes))
    assert result == [(1, 2), (3, 4), (5, 6), (7, 8)]

def test_group_indexes_empty():
    indexes = []
    result = list(group_indexes(indexes))
    assert result == []

def test_group_indexes_single_element():
    indexes = [2]
    result = list(group_indexes(indexes))
    assert result == [(2, 3)]


@pytest.fixture
def built_ontology_basic():
    with open('ontology_test_data/built_ontology_basic.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def built_ontology_complex():
    with open('ontology_test_data/built_ontology_complex.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def id_to_label_basic():
    return {0: "Bird sounds", 1: "Mammal sounds", 2: "Dog barks"}

@pytest.fixture
def id_to_label_complex():
    return {0: "Bicycle", 1: "Car sounds", 2: "Engine", 3: "Horn", 4: "Tires", 5: "Gears"}

@pytest.fixture
def event_finder_basic(built_ontology_basic, id_to_label_basic):
    return EventFinder(id_to_label=id_to_label_basic, 
                       events_graph=built_ontology_basic, 
                       chunk_length_sec=5, 
                       probability_threshold=0.5)

@pytest.fixture
def event_finder_complex(built_ontology_complex, id_to_label_complex):
    return EventFinder(id_to_label=id_to_label_complex, 
                       events_graph=built_ontology_complex, 
                       chunk_length_sec=5, 
                       probability_threshold=0.5)

def test_find_single_event_basic(event_finder_basic):
    segments_probs = np.array([[0.6, 0.1, 0.0],
                               [0.7, 0.2, 0.0],
                               [0.4, 0.4, 0.1]])
    expected = [("Bird sounds", 0, 10)]
    result = event_finder_basic.find(segments_probs)
    assert result == expected

def test_find_multiple_events_basic(event_finder_basic):
    segments_probs = np.array([[0.6, 0.1, 0.0],
                               [0.7, 0.2, 0.0],
                               [0.4, 0.6, 0.7]])
    expected = [("Bird sounds", 0, 10), ("Dog barks", 10, 15)]
    result = event_finder_basic.find(segments_probs)
    assert result == expected

def test_find_with_gap_basic(event_finder_basic):
    segments_probs = np.array([[0.6, 0.1, 0.0],
                               [0.0, 0.0, 0.0],
                               [0.7, 0.2, 0.0],
                               [0.4, 0.6, 0.7]])
    expected = [("Bird sounds", 0, 5), ("Bird sounds", 10, 15), ("Dog barks", 15, 20)]
    result = event_finder_basic.find(segments_probs)
    assert result == expected

def test_find_no_events_basic(event_finder_basic):
    segments_probs = np.array([[0.1, 0.1, 0.0],
                               [0.2, 0.2, 0.1],
                               [0.4, 0.4, 0.4]])
    expected = []
    result = event_finder_basic.find(segments_probs)
    assert result == expected

def test_find_leave_ancestor_basic(event_finder_basic):
    segments_probs = np.array([[0.1, 0.6, 0.0],
                               [0.2, 0.7, 0.0],
                               [0.3, 0.8, 0.0]])
    expected = [("Mammal sounds", 0, 15)]
    result = event_finder_basic.find(segments_probs)
    assert result == expected

def test_find_single_event_complex(event_finder_complex):
    segments_probs = np.array([[0.6, 0.1, 0.0, 0.0, 0.0, 0.0],
                               [0.7, 0.2, 0.0, 0.0, 0.0, 0.0],
                               [0.4, 0.4, 0.1, 0.0, 0.0, 0.0]])
    expected = [("Bicycle", 0, 10)]
    result = event_finder_complex.find(segments_probs)
    assert result == expected

def test_find_multiple_events_complex(event_finder_complex):
    segments_probs = np.array([[0.6, 0.1, 0.0, 0.0, 0.0, 0.0],
                               [0.7, 0.2, 0.0, 0.0, 0.0, 0.0],
                               [0.4, 0.6, 0.1, 0.7, 0.0, 0.0]])
    expected = [("Bicycle", 0, 10), ("Horn", 10, 15)]
    result = event_finder_complex.find(segments_probs)
    assert result == expected

def test_find_with_gap_complex(event_finder_complex):
    segments_probs = np.array([[0.6, 0.1, 0.0, 0.0, 0.0, 0.0],
                               [0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                               [0.7, 0.2, 0.0, 0.0, 0.0, 0.0],
                               [0.4, 0.6, 0.1, 0.7, 0.0, 0.0]])
    expected = [("Bicycle", 0, 5), ("Bicycle", 10, 15), ("Horn", 15, 20)]
    result = event_finder_complex.find(segments_probs)
    assert result == expected

def test_find_no_events_complex(event_finder_complex):
    segments_probs = np.array([[0.1, 0.1, 0.0, 0.0, 0.0, 0.0],
                               [0.2, 0.2, 0.1, 0.0, 0.0, 0.0],
                               [0.4, 0.4, 0.4, 0.0, 0.0, 0.0]])
    expected = []
    result = event_finder_complex.find(segments_probs)
    assert result == expected

def test_find_high_probabilities_multiple_parents_tires(event_finder_complex):
    segments_probs = np.array([[0.0, 0.6, 0.3, 0.2, 0.95, 0.3],
                               [0.0, 0.7, 0.15, 0.12, 0.96, 0.2],
                               [0.0, 0.75, 0.48, 0.44, 0.97, 0.4]])

    expected = [("Tires", 0, 15)]  # Tires should be identified, ancestors "Bicycle" and "Car sounds" are removed
    result = event_finder_complex.find(segments_probs)
    assert result == expected


if __name__ == '__main__':
    pytest.main()
