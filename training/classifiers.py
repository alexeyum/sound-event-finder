import torch.nn as nn
import torch.nn.functional as F


class BasicConvNet(nn.Module):
    def __init__(self, input_channels, output_size, 
                 first_channels=4, inner_channels=16, final_features=16):
        super().__init__()

        self.features = nn.Sequential(
            nn.Conv1d(input_channels, first_channels, kernel_size=80, stride=64, bias=False),
            nn.BatchNorm1d(first_channels),
            nn.ReLU(),
            nn.Conv1d(first_channels, inner_channels, kernel_size=3, stride=4, bias=False),
            nn.BatchNorm1d(inner_channels),
            nn.ReLU(),
            nn.Conv1d(inner_channels, inner_channels, kernel_size=3, stride=4, bias=False),
            nn.BatchNorm1d(inner_channels),
            nn.ReLU(),
            nn.AdaptiveMaxPool1d(final_features))

        self.classifier = nn.Linear(inner_channels * final_features, output_size)


    def forward(self, x):
        features = self.features(x)
        logits = self.classifier(features.reshape(features.shape[0], -1))
        return F.log_softmax(logits, dim=-1)


class ResidualConv1dBlock(nn.Module):
    def __init__(self, in_channels, out_channels, stride=1):
        super().__init__()
        self.layers = nn.Sequential(
            nn.Conv1d(in_channels, out_channels, kernel_size=3,
                      stride=stride, bias=False, padding=1),
            nn.BatchNorm1d(out_channels),
            nn.ReLU(),
            nn.Conv1d(out_channels, out_channels,
                      kernel_size=3, bias=False, padding=1),
            nn.BatchNorm1d(out_channels),
        )

        if in_channels != out_channels or stride != 1:
            self.shortcut = nn.Sequential(
                nn.Conv1d(in_channels, out_channels, kernel_size=1,
                          stride=stride, bias=False),
                nn.BatchNorm1d(out_channels),
            )
        else:
            self.shortcut = nn.Identity()


    def forward(self, x):
        return F.relu(self.layers(x) + self.shortcut(x))


# NB: not used yet
class BottleneckConv1dBlock(nn.Module):
    def __init__(self, in_channels, out_channels, bottleneck_channels, stride=1):
        super().__init__()
        self.layers = nn.Sequential(
            nn.Conv1d(in_channels, bottleneck_channels, kernel_size=1, bias=False),
            nn.BatchNorm1d(bottleneck_channels),
            nn.ReLU(),
            nn.Conv1d(bottleneck_channels, bottleneck_channels, kernel_size=3,
                      stride=stride, bias=False, padding=1),
            nn.BatchNorm1d(bottleneck_channels),
            nn.ReLU(),
            nn.Conv1d(bottleneck_channels, out_channels, kernel_size=1, bias=False),
            nn.BatchNorm1d(out_channels),
        )

        if in_channels != out_channels or stride != 1:
            self.shortcut = nn.Sequential(
                nn.Conv1d(in_channels, out_channels, kernel_size=1,
                          stride=stride, bias=False),
                nn.BatchNorm1d(out_channels),
            )
        else:
            self.shortcut = nn.Identity()

    def forward(self, x):
        return F.relu(self.layers(x) + self.shortcut(x))


class AudioResNet(nn.Module):
    def __init__(self, input_channels, output_size, layers_args):
        super().__init__()

        hidden, kernel, stride = layers_args[0]
        layers = [
            nn.Conv1d(input_channels, hidden, kernel_size=kernel, stride=stride, bias=False),
            nn.BatchNorm1d(hidden),
            nn.ReLU(),
        ]
        for args in layers_args[1:-1]:
            layers.append(ResidualConv1dBlock(*args))

        hidden, pool = layers_args[-1]
        layers.append(nn.AdaptiveMaxPool1d(pool))

        self.features = nn.Sequential(*layers)

        self.classifier = nn.Linear(hidden * pool, output_size)

    def forward(self, x):
        features = self.features(x)
        logits = self.classifier(features.reshape(features.shape[0], -1))
        return F.log_softmax(logits, dim=-1)