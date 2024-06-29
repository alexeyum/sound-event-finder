from copy import copy

import ontology


def group_indexes(indexes):

    """Groups consecutive indexes into ranges.

    This function takes a list of indexes and yields tuples representing
    ranges of consecutive indexes. Each tuple contains the start of the range
    and one past the end of the range (i.e., half-open intervals).

    Args:
        indexes (iterable of int): A sorted iterable of indexes to group into ranges.

    Yields:
        tuple: A tuple (start, end) where 'start' is the beginning of a consecutive
               range and 'end' is one past the last index in that range.

    Example:
        indexes = [1, 2, 3, 5, 6, 8]
        list(group_indexes(indexes))  # Output: [(1, 4), (5, 7), (8, 9)]
    """

    begin = None
    prev = None
    for ind in indexes:
        if begin is None:
            begin = ind
            prev = ind
        else:
            if ind == prev + 1:
                prev = ind
            else:
                yield begin, prev + 1
                begin = ind
                prev = ind

    if begin is not None:
        yield begin, prev + 1


class EventFinder:

    """A class for finding events based on probability thresholds and event hierarchies.

    The `EventFinder` class uses events graph to identify and group events from a sequence 
    of probability distributions. Events are identified based on a probability threshold 
    and organized into time ranges, with ancestor events being removed to avoid duplication.

    Attributes:
        id_to_label (object): Mapping from indexes to labels in the model probability array.
        events_graph (dict): A dictionary representing the hierarchy of events.
        chunk_length_sec (int): The length of each time chunk in seconds.
        probability_threshold (float): The minimum probability required to consider an event.

    Methods:
        find(segments_probs):
            Identifies and groups events based on the provided probability distributions.
    """

    def __init__(self, id_to_label, events_graph, chunk_length_sec, probability_threshold):
        self.id_to_label = id_to_label
        self.chunk_length_sec = chunk_length_sec
        self.probability_threshold = probability_threshold
        self.events_graph = events_graph

    def _remove_ancestor_events(self, probabilities):
        new_probs = copy(probabilities)
        for event in probabilities.keys():
            for ancestor in ontology.get_all_ancestors(self.events_graph, event):
                if ancestor in new_probs:
                    del new_probs[ancestor]
        return new_probs

    @staticmethod
    def _group_events(events):
        events_positions = {}
        for pos, pos_events in enumerate(events):
            for event in pos_events:
                if event not in events_positions:
                    events_positions[event] = []
                events_positions[event].append(pos)

        events_grouped = []
        for event, positions in events_positions.items():
            for start, end in group_indexes(positions):
                events_grouped.append([start, end, event])
        return events_grouped

    def _find_events_ranges(self, segments_probs):
        events = []
        for segment_index in range(segments_probs.shape[0]):

            prob_dict = {self.id_to_label[i]: segments_probs[segment_index, i] 
                         for i in range(segments_probs.shape[1])}

            prob_dict = {event: prob for event, prob in prob_dict.items() 
                         if (event in self.events_graph and prob >= self.probability_threshold)}

            prob_dict = self._remove_ancestor_events(prob_dict)

            events.append(list(prob_dict.keys()))

        return self._group_events(events)

    def find(self, segments_probs):

        """Identifies and groups events based on the provided probability distributions.

        This method processes the probability distributions of events over time segments,
        groups consecutive occurrences of events into ranges, and returns the results
        as a list of tuples containing event names and their time ranges in seconds.

        Args:
            segments_probs (numpy.ndarray): A 2D array of probability distributions,
                                            where each row represents a time segment
                                            and each column represents an event class.

        Returns:
            list: A sorted list of tuples, each containing an event name, start time,
                  and end time in seconds.
        """

        events_ranges = self._find_events_ranges(segments_probs)

        events = []
        for begin, end, event_name in events_ranges:
            begin_sec = begin * self.chunk_length_sec
            end_sec = end * self.chunk_length_sec
            events.append((event_name, begin_sec, end_sec))

        # sort by the segment start
        events.sort(key=lambda e: e[1])

        return events
