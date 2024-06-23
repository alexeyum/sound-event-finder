ROOT_NAME = '<root>'


def build_ontology_tree(ontology_data, model_classes):
    model_classes = set(model_classes)

    id_to_obj = {}
    for el in ontology_data:
        id_to_obj[el['id']] = el

    name_to_node = {}
    for el in ontology_data:
        name_to_node[el['name']] = {
            'children': [id_to_obj[id]['name'] for id in el['child_ids']],
            'abstract': 'abstract' in el['restrictions'],
            'blacklist': 'blacklist' in el['restrictions'],
            'not_in_model': el['name'] not in model_classes,
        }

    top_level = set(name_to_node.keys())
    for node in name_to_node.values():
        top_level -= set(node['children'])

    name_to_node[ROOT_NAME] = {
        'children': list(top_level),
        'abstract': True,
        'blacklist': False,
        'not_in_model': True,
    }

    return name_to_node


def remove_node_subtree(nodes, parent_name, name):
    for child_name in list(nodes[name]['children']):
        remove_node_subtree(nodes, name, child_name)
    nodes[parent_name]['children'].remove(name)
    del nodes[name]


def remove_by_property(nodes, property_name, property_value=True):
    def remove_by_property_recursive(name):
        for child_name in list(nodes[name]['children']):  # copy for correctness
            if child_name not in nodes:  # duplicate reference, already deleted
                nodes[name]['children'].remove(child_name)
            else:
                if (not nodes[child_name]['abstract'] and 
                        nodes[child_name][property_name] == property_value):
                    remove_node_subtree(nodes, name, child_name)
                else:
                    remove_by_property_recursive(child_name)

    remove_by_property_recursive(ROOT_NAME)

def drop_property(nodes, property_name):
    for name, node in nodes.items():
        if property_name in node:
            del node[property_name]
