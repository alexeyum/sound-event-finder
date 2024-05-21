from event_finder import _continuos_segments


def test_continuos_segments_simple():
    inp = [True, True, False]
    expected = [(0, 2)]
    assert list(_continuos_segments(inp)) == expected

    inp = [True, False, True, True, False, False, False, True]
    expected = [(0, 1), (2, 4), (7, 8)]
    assert list(_continuos_segments(inp)) == expected

    inp = [False, False, False, True, False, True, True, True, False]
    expected = [(3, 4), (5, 8)]
    assert list(_continuos_segments(inp)) == expected


def test_continuos_segments_edge():
    inp = []
    expected = []
    assert list(_continuos_segments(inp)) == expected

    inp = [True]
    expected = [(0, 1)]
    assert list(_continuos_segments(inp)) == expected

    inp = [False]
    expected = []
    assert list(_continuos_segments(inp)) == expected

    inp = [True, True, True, True]
    expected = [(0, 4)]
    assert list(_continuos_segments(inp)) == expected

    inp = [False, False, False, False]
    expected = []
    assert list(_continuos_segments(inp)) == expected