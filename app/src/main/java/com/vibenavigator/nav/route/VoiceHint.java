package com.vibenavigator.nav.route;

public final class VoiceHint {
    public final int indexInTrack;
    public final int command;
    public final int exitNumber;
    public final double distanceToNextMeters;
    public final int angleDegrees;

    public VoiceHint(int indexInTrack, int command, int exitNumber, double distanceToNextMeters, int angleDegrees) {
        this.indexInTrack = indexInTrack;
        this.command = command;
        this.exitNumber = exitNumber;
        this.distanceToNextMeters = distanceToNextMeters;
        this.angleDegrees = angleDegrees;
    }
}
