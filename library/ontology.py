from copy import copy
import sys


ROOT_NAME = '<root>'


def _create_nodes(ontology_data, model_classes):
    model_classes = set(model_classes)

    id_to_obj = {}
    for el in ontology_data:
        id_to_obj[el['id']] = el

    name_to_node = {}
    for el in ontology_data:
        name_to_node[el['name']] = {
            'children': sorted([id_to_obj[id]['name'] for id in el['child_ids']]),
            'parents': [],  # will be filled later
            'abstract': 'abstract' in el['restrictions'],
            'blacklist': 'blacklist' in el['restrictions'],
            'in_model': el['name'] in model_classes,
        }
    return name_to_node


def _add_root(name_to_node):
    top_level = set(name_to_node.keys())
    for name, node in name_to_node.items():
        top_level -= set(node['children'])

    name_to_node[ROOT_NAME] = {
        'children': list(top_level),
        'parents': [],
        'abstract': True,
        'blacklist': False,
        'in_model': False,
    }


def _assign_parents(name_to_node):
    for name, node in name_to_node.items():
        for child in node['children']:
            name_to_node[child]['parents'].append(name)


def _sort_names(name_to_node):
    for node in name_to_node.values():
        node['children'] = sorted(node['children'])
        node['parents'] = sorted(node['parents'])


def build_ontology_graph(ontology_data, model_classes, sort_names=False):

    """Constructs an ontology graph from given data.

    This function takes raw ontology data and builds a more convenient
    hierarchical structure based on provided ontology data and model classes.
    
    Args:
        ontology_data (list of dict): A list of dictionaries where each dictionary
            represents an entity with the following keys:
            - 'id' (str): Unique identifier of the entity.
            - 'name' (str): Name of the entity.
            - 'child_ids' (list of str): List of child entity IDs.
            - 'restrictions' (list of str): List of restrictions on the entity 
            (e.g., 'abstract', 'blacklist').
        model_classes (list of str): A list of class names that are part of the model.
        sort_names (bool, optional): If True, sorts the children and parents lists
            for each node alphabetically for consistency. Default is False.
    
    Returns:
        dict: A dictionary where keys are entity names and values are dictionaries
            containing the following keys:
            - 'children' (list of str): List of names of child entities.
            - 'parents' (list of str): List of names of parent entities.
            - 'abstract' (bool): Indicates if the entity is abstract.
            - 'blacklist' (bool): Indicates if the entity is blacklisted.
            - 'in_model' (bool): Indicates if the entity is part of the model classes.
    
    Note:
        A root node is added to the graph to serve as the top-level parent for all
        entities that do not have any parents. This root node is named by the constant
        `ROOT_NAME` and is marked as abstract, not blacklisted, and not part of the model.
    """

    name_to_node = _create_nodes(ontology_data, model_classes)
    _add_root(name_to_node)
    _assign_parents(name_to_node)

    # for consistency in tests
    if sort_names: 
        _sort_names(name_to_node)

    return name_to_node


def remove_link(name_to_node, parent, child):

    """Removes a directed link in graph, then drops all orphan nodes recursively.

    This function removes the directed edge from the parent to the child in the
    graph. If the child becomes an orphan (has no parents), the function recursively 
    removes the child and all its descendants (if they become orphaned).

    Args:
        name_to_node (dict): The ontology graph represented as a dictionary of nodes.
        parent (str): The name of the parent node.
        child (str): The name of the child node.
    """

    name_to_node[parent]['children'].remove(child)
    name_to_node[child]['parents'].remove(parent)

    if not name_to_node[child]['parents']:
        for sub_child in copy(name_to_node[child]['children']):
            remove_link(name_to_node, child, sub_child)
        del name_to_node[child]


def remove_node(name_to_node, node):

    """Removes a node and all links to it from the graph.

    This function removes a specified node from the graph along with all connections
    leading to it. The node itself and any orphaned descendants are removed recursively.

    Args:
        name_to_node (dict): The ontology graph represented as a dictionary of nodes.
        node (str): The name of the node to be removed.

    Note: deleting root node is not allowed because it should just delete
    the whole graph, and is probably a mistake.
    """

    if node == ROOT_NAME:
        raise ValueError("Cannot delete root node")

    for parent in copy(name_to_node[node]['parents']):
        remove_link(name_to_node, parent, node)


def print_as_tree(nodes, file=sys.stdout):

    """Prints the ontology graph as a tree structure.

    This function outputs the hierarchical structure of the ontology graph,
    starting from the root node and printing each node and its children recursively.
    Nodes marked as 'abstract' or having multiple parents are indicated with
    corresponding annotations.

    Args:
        nodes (dict): A dictionary representing the ontology graph. Each key is
            a node name, and its value is a dictionary with the following keys:
            - 'children' (list of str): Names of child nodes.
            - 'parents' (list of str): Names of parent nodes.
            - 'abstract' (bool): Whether the node is abstract.
        file (file-like object, optional): The file to which the tree should be printed.
            Defaults to sys.stdout.

    Note:
        The root node is identified by the constant `ROOT_NAME`.
    """

    def print_recursive(name, indent):
        add_str = ""

        if nodes[name]['abstract']:
            add_str += " (abstract)"
        if len(nodes[name]['parents']) > 1:
            add_str += " (multiple parents)"

        print(indent + name + add_str, file=file)
        for child_name in nodes[name]['children']:
            print_recursive(child_name, indent + '- ')

    for child_name in nodes[ROOT_NAME]['children']:
        print_recursive(child_name, indent='')


def drop_property(nodes, property_name):

    """Removes a specified property from all nodes in the ontology graph.

    Args:
        nodes (dict): A dictionary representing the ontology graph. Each key is
            a node name, and its value is a dictionary of node properties.
        property_name (str): The name of the property to be removed from each node.
    """

    for name, node in nodes.items():
        if property_name in node:
            del node[property_name]
