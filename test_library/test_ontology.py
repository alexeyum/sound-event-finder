import pytest

from ontology import *


# Fake input data
ontology_data = [
    {
        "id": "/m/0a",
        "name": "Animal sounds",
        "description": "Sounds produced by animals.",
        "citation_uri": "",
        "positive_examples": [],
        "child_ids": ["/m/0b", "/m/0c"],
        "restrictions": ["abstract"]
    },
    {
        "id": "/m/0b",
        "name": "Bird sounds",
        "description": "Sounds produced by birds.",
        "citation_uri": "",
        "positive_examples": [],
        "child_ids": [],
        "restrictions": ["blacklist"]
    },
    {
        "id": "/m/0c",
        "name": "Mammal sounds",
        "description": "Sounds produced by mammals.",
        "citation_uri": "",
        "positive_examples": [],
        "child_ids": ["/m/0d"],
        "restrictions": []
    },
    {
        "id": "/m/0d",
        "name": "Dog barks",
        "description": "Barking sounds made by dogs.",
        "citation_uri": "",
        "positive_examples": [],
        "child_ids": [],
        "restrictions": []
    }
]

# Fake model classes
model_classes = ["Animal sounds", "Dog barks"]

def test_basic_functionality():
    expected_output = {
        '<root>': {
            'children': ['Animal sounds'],
            'abstract': True,
            'blacklist': False,
            'not_in_model': True,
        },
        'Animal sounds': {
            'children': ['Bird sounds', 'Mammal sounds'],
            'abstract': True,
            'blacklist': False,
            'not_in_model': False,
        },
        'Bird sounds': {
            'children': [],
            'abstract': False,
            'blacklist': True,
            'not_in_model': True,
        },
        'Mammal sounds': {
            'children': ['Dog barks'],
            'abstract': False,
            'blacklist': False,
            'not_in_model': True,
        },
        'Dog barks': {
            'children': [],
            'abstract': False,
            'blacklist': False,
            'not_in_model': False,
        }
    }
    result = build_ontology_tree(ontology_data, model_classes)
    assert result == expected_output

def test_complex_tree_structure():
    complex_ontology_data = [
        {
            "id": "/m/0a",
            "name": "Vehicle sounds",
            "description": "Sounds produced by vehicles.",
            "citation_uri": "",
            "positive_examples": [],
            "child_ids": ["/m/0b", "/m/0c"],
            "restrictions": ["abstract"]
        },
        {
            "id": "/m/0b",
            "name": "Car sounds",
            "description": "Sounds produced by cars.",
            "citation_uri": "",
            "positive_examples": [],
            "child_ids": [],
            "restrictions": []
        },
        {
            "id": "/m/0c",
            "name": "Bike sounds",
            "description": "Sounds produced by bikes.",
            "citation_uri": "",
            "positive_examples": [],
            "child_ids": ["/m/0d"],
            "restrictions": []
        },
        {
            "id": "/m/0d",
            "name": "Motorcycle sounds",
            "description": "Sounds produced by motorcycles.",
            "citation_uri": "",
            "positive_examples": [],
            "child_ids": [],
            "restrictions": []
        }
    ]
    result = build_ontology_tree(complex_ontology_data, model_classes)
    assert result['Vehicle sounds']['children'] == ['Car sounds', 'Bike sounds']

def test_empty_ontology_list():
    result = build_ontology_tree([], model_classes)
    expected_output = {
        '<root>': {
            'children': [],
            'abstract': True,
            'blacklist': False,
            'not_in_model': True,
        }
    }
    assert result == expected_output


@pytest.fixture
def ontology_tree():
    return build_ontology_tree(ontology_data, model_classes)

def test_remove_leaf_node(ontology_tree):
    remove_node_subtree(ontology_tree, "Mammal sounds", "Dog barks")
    assert "Dog barks" not in ontology_tree
    assert "Dog barks" not in ontology_tree["Mammal sounds"]["children"]

def test_remove_node_with_children(ontology_tree):
    remove_node_subtree(ontology_tree, "Animal sounds", "Mammal sounds")
    assert "Mammal sounds" not in ontology_tree
    assert "Mammal sounds" not in ontology_tree["Animal sounds"]["children"]
    assert "Dog barks" not in ontology_tree

def test_remove_root_child(ontology_tree):
    remove_node_subtree(ontology_tree, ROOT_NAME, "Animal sounds")
    assert "Animal sounds" not in ontology_tree
    assert "Animal sounds" not in ontology_tree[ROOT_NAME]["children"]
    assert "Bird sounds" not in ontology_tree
    assert "Mammal sounds" not in ontology_tree
    assert "Dog barks" not in ontology_tree

def test_remove_non_existent_node(ontology_tree):
    with pytest.raises(KeyError):
        remove_node_subtree(ontology_tree, "Animal sounds", "Non-existent sound")


def test_remove_by_property_not_in_model(ontology_tree):
    remove_by_property(ontology_tree, 'not_in_model', True)
    assert 'Animal sounds' in ontology_tree
    assert 'Bird sounds' not in ontology_tree
    assert 'Mammal sounds' not in ontology_tree
    assert 'Dog barks' not in ontology_tree   # child of dropped

def test_remove_by_property_blacklist(ontology_tree):
    remove_by_property(ontology_tree, 'blacklist', True)
    assert 'Bird sounds' not in ontology_tree
    assert 'Animal sounds' in ontology_tree
    assert 'Mammal sounds' in ontology_tree
    assert 'Dog barks' in ontology_tree

def test_remove_by_property_with_different_value(ontology_tree):
    # Test a scenario where property_value is False
    remove_by_property(ontology_tree, 'abstract', False)
    assert 'Animal sounds' in ontology_tree
    assert 'Bird sounds' not in ontology_tree
    assert 'Mammal sounds' not in ontology_tree
    assert 'Dog barks' not in ontology_tree


def test_drop_property(ontology_tree):
    drop_property(ontology_tree, 'abstract')
    for node in ontology_tree.values():
        assert 'abstract' not in node


if __name__ == '__main__':
    pytest.main()
