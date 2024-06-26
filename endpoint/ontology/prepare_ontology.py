import json

import ontology


def _manual_properties_fixes(nodes):
    nodes['Sounds of things']['abstract'] = True
    nodes['Sounds of things']['blacklist'] = False

    nodes['Natural sounds']['abstract'] = True
    nodes['Natural sounds']['blacklist'] = False

    nodes['Microphone']['abstract'] = True
    nodes['Microphone']['blacklist'] = False

    nodes['Domestic sounds, home sounds']['abstract'] = True
    nodes['Domestic sounds, home sounds']['blacklist'] = False

    nodes['Non-motorized land vehicle']['abstract'] = True


def _build_graph():
    with open('raw_ontology_data.json') as f:
        ontology_raw = json.load(f)

    with open('model_classes.txt') as f:
        model_classes = f.read().strip().split('\n')

    return ontology.build_ontology_graph(ontology_raw, model_classes)


def _remove_blacklisted(nodes):
    for name in list(nodes.keys()):
        if name not in nodes:  # alread deleted
            continue
        if nodes[name]['blacklist']:
            ontology.remove_node(nodes, name)

    ontology.drop_property(nodes, 'blacklist')

def _remove_non_model(nodes):
    for name in list(nodes.keys()):
        if name not in nodes:  # alread deleted
            continue
        if not nodes[name]['abstract'] and not nodes[name]['in_model']:
            ontology.remove_node(nodes, name)
    ontology.drop_property(nodes, 'in_model')


def main():
    nodes = _build_graph()
    
    # some properties seem to be incorrect
    _manual_properties_fixes(nodes)

    # this subtree have lot's of problems and doesn't seem useful
    ontology.remove_node(nodes, 'Channel, environment and background')

    # not working with blacklisted nodes
    _remove_blacklisted(nodes)

    # nodes not in model are not necessary
    _remove_non_model(nodes)


    with open('ontology.json', 'w') as f:
        json.dump(nodes, f, indent=4)

    with open('ontology_tree.txt', 'w') as f:
        ontology.print_as_tree(nodes, f)


if __name__ == "__main__":
    main()
