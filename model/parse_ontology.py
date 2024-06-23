import json

import ontology


def make_manual_fixes(nodes):
    nodes['Sounds of things']['abstract'] = True
    nodes['Sounds of things']['blacklist'] = False

    nodes['Natural sounds']['abstract'] = True
    nodes['Natural sounds']['blacklist'] = False

    nodes['Microphone']['abstract'] = True
    nodes['Microphone']['blacklist'] = False

    nodes['Domestic sounds, home sounds']['abstract'] = True
    nodes['Domestic sounds, home sounds']['blacklist'] = False

    nodes['Non-motorized land vehicle']['abstract'] = True

    ontology.remove_node_subtree(nodes, ontology.ROOT_NAME, 'Channel, environment and background')


def pretty_print_tree(nodes, file):
    def pp_recursive(name, indent):
        if nodes[name]['abstract']:
            abs_str = " (abstract)"
        else:
            abs_str = ""

        print(indent + name + abs_str, file=file)
        for child_name in nodes[name]['children']:
            pp_recursive(child_name, indent + '- ')

    for child_name in nodes[ontology.ROOT_NAME]['children']:
        pp_recursive(child_name, indent='')


def main():
    with open('ontology_raw.json') as f:
        ontology_raw = json.load(f)

    with open('ast_classes.txt') as f:
        model_classes = f.read().strip().split('\n')

    nodes = ontology.build_ontology_tree(ontology_raw, model_classes)

    make_manual_fixes(nodes)

    ontology.remove_by_property(nodes, 'blacklist')
    ontology.drop_property(nodes, 'blacklist')
    ontology.remove_by_property(nodes, 'not_in_model')
    ontology.drop_property(nodes, 'not_in_model')

    with open('ontology.json', 'w') as f:
        json.dump(nodes, f, indent=4)

    with open('ontology_tree.txt', 'w') as f:
        pretty_print_tree(nodes, f)


if __name__ == "__main__":
    main()
