import pytest
import json
from io import StringIO
from textwrap import dedent

from ontology import *


@pytest.fixture
def raw_ontology_basic():
    with open('ontology_test_data/raw_ontology_basic.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def built_ontology_basic():
    with open('ontology_test_data/built_ontology_basic.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def model_classes_basic():
    return ["Bird sounds", "Mammal sounds"]


@pytest.fixture
def raw_ontology_complex():
    with open('ontology_test_data/raw_ontology_complex.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def built_ontology_complex():
    with open('ontology_test_data/built_ontology_complex.json', 'r') as f:
        return json.load(f)

@pytest.fixture
def model_classes_complex():
    return ["Horn", "Engine", "Gears", "Building", "Tools"]


def test_ontology_basic(raw_ontology_basic, built_ontology_basic, model_classes_basic):
    nodes = build_ontology_graph(raw_ontology_basic, model_classes_basic, sort_names=True)
    assert nodes == built_ontology_basic

def test_ontology_complex(raw_ontology_complex, built_ontology_complex, model_classes_complex):
    nodes = build_ontology_graph(raw_ontology_complex, model_classes_complex, sort_names=True)
    assert nodes == built_ontology_complex

def test_ontology_empty():
    result = build_ontology_graph([], [])
    expected_output = {
        '<root>': {
            'children': [],
            'parents': [],
            'abstract': True,
            'blacklist': False,
            'in_model': False,
        }
    }
    assert result == expected_output


def test_remove_link_basic_1(built_ontology_basic):
    nodes = built_ontology_basic
    remove_link(nodes, "Animal sounds", "Mammal sounds")
    assert "Mammal sounds" not in nodes
    assert "Dog barks" not in nodes
    assert "Mammal sounds" not in nodes["Animal sounds"]['children']

def test_remove_link_basic_2(built_ontology_basic):
    nodes = built_ontology_basic
    remove_link(nodes, ROOT_NAME, "Animal sounds")
    assert "Animal sounds" not in nodes
    assert "Mammal sounds" not in nodes
    assert "Bird sounds" not in nodes
    assert "Dog barks" not in nodes
    assert not nodes[ROOT_NAME]['children']

def test_remove_link_multiple_parents(built_ontology_complex):
    nodes = built_ontology_complex
    remove_link(nodes, "Car sounds", "Tires")
    assert "Tires" in nodes
    assert "Tires" not in nodes["Car sounds"]["children"]

def test_remove_link_complex(built_ontology_complex):
    nodes = built_ontology_complex
    remove_link(nodes, "Industrial sounds", "Mechanisms")
    assert "Mechanisms" not in nodes
    assert "Pulleys" not in nodes
    assert "Gears" in nodes
    assert "Mechanisms" not in nodes["Gears"]['parents']
    assert "Mechanisms" not in nodes["Industrial sounds"]['children']

def test_remove_node_basic_1(built_ontology_basic):
    nodes = built_ontology_basic
    remove_node(nodes, "Mammal sounds")
    assert "Mammal sounds" not in nodes
    assert "Dog barks" not in nodes
    assert "Mammal sounds" not in nodes["Animal sounds"]['children']

def test_remove_node_basic_2(built_ontology_basic):
    nodes = built_ontology_basic
    remove_node(nodes, "Bird sounds")
    assert "Bird sounds" not in nodes
    assert "Bird sounds" not in nodes["Animal sounds"]['children']

def test_remove_node_multiple_parents(built_ontology_complex):
    nodes = built_ontology_complex
    remove_node(nodes, "Gears")
    assert "Gears" not in nodes
    assert "Gears" not in nodes["Bicycle"]["children"]
    assert "Gears" not in nodes["Mechanisms"]["children"]

def test_remove_node_complex(built_ontology_complex):
    nodes = built_ontology_complex
    remove_node(nodes, "Bicycle")
    assert "Bicycle" not in nodes
    assert "Gears" in nodes
    assert "Tires" in nodes
    assert "Bicycle" not in nodes["Gears"]['parents']
    assert "Bicycle" not in nodes["Tires"]['parents']
    assert "Bicycle" not in nodes["Vehicle sounds"]['children']

def test_remove_node_root(built_ontology_complex):
    nodes = built_ontology_complex
    with pytest.raises(ValueError):
        remove_node(nodes, ROOT_NAME)


def test_print_as_tree_basic(built_ontology_basic):
    output = StringIO()
    print_as_tree(built_ontology_basic, output)
    expected = dedent("""
        Animal sounds (abstract)
        - Bird sounds
        - Mammal sounds
        - - Dog barks
    """).lstrip()
    assert output.getvalue() == expected

def test_print_as_tree_complex(built_ontology_complex):
    output = StringIO()
    print_as_tree(built_ontology_complex, output)
    expected = dedent("""
        Industrial sounds (abstract)
        - Building
        - - Tools
        - Mechanisms (abstract)
        - - Gears (multiple parents)
        - - Pulleys
        Vehicle sounds (abstract)
        - Bicycle
        - - Gears (multiple parents)
        - - Tires (multiple parents)
        - Car sounds (abstract)
        - - Engine
        - - Horn
        - - Tires (multiple parents)
    """).lstrip()
    assert output.getvalue() == expected

def test_drop_property_existing_property():
    nodes = {
        'A': {'children': ['B'], 'parents': [], 'abstract': False, 'blacklist': False, 'in_model': True},
        'B': {'children': [], 'parents': ['A'], 'abstract': False, 'blacklist': True, 'in_model': True}
    }
    drop_property(nodes, 'blacklist')
    
    assert 'blacklist' not in nodes['A']
    assert 'blacklist' not in nodes['B']
    assert 'children' in nodes['A']
    assert 'in_model' in nodes['B']

def test_drop_property_non_existent_property():
    nodes = {
        'A': {'children': ['B'], 'parents': [], 'abstract': False, 'blacklist': False, 'in_model': True},
        'B': {'children': [], 'parents': ['A'], 'abstract': False, 'blacklist': True, 'in_model': True}
    }
    drop_property(nodes, 'non_existent_property')
    
    assert 'children' in nodes['A']
    assert 'blacklist' in nodes['B']
    assert 'non_existent_property' not in nodes['A']
    assert 'non_existent_property' not in nodes['B']
    assert 'in_model' in nodes['A']
    assert 'abstract' in nodes['B']


def test_get_all_ancestors_basic_single(built_ontology_basic):
    result = set(get_all_ancestors(built_ontology_basic, 'Bird sounds'))
    assert result == {'Animal sounds', '<root>'}

def test_get_all_ancestors_basic_multiple(built_ontology_basic):
    result = set(get_all_ancestors(built_ontology_basic, 'Dog barks'))
    assert result == {'Mammal sounds', 'Animal sounds', '<root>'}

def test_get_all_ancestors_complex_single(built_ontology_complex):
    result = set(get_all_ancestors(built_ontology_complex, 'Horn'))
    assert result == {'Car sounds', 'Vehicle sounds', '<root>'}

def test_get_all_ancestors_complex_multiple_parents(built_ontology_complex):
    result = set(get_all_ancestors(built_ontology_complex, 'Tires'))
    assert result == {'Bicycle', 'Vehicle sounds', '<root>', 'Car sounds'}

def test_get_all_ancestors_complex_no_parents(built_ontology_complex):
    result = set(get_all_ancestors(built_ontology_complex, '<root>'))
    assert result == set()


if __name__ == '__main__':
    pytest.main()
