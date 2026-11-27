package be.maarten.woolworks.machine;

public enum MuffleMode {
    OFF, MACHINE, SHEEP, BOTH;
    public boolean machineMuted() { return this == MACHINE || this == BOTH; }
    public boolean sheepMuted() { return this == SHEEP || this == BOTH; }
    public static MuffleMode read(int value, boolean supportsSheep) {
        return value >= 0 && value < (supportsSheep ? 4 : 2) ? values()[value] : OFF;
    }
    public MuffleMode next(boolean supportsSheep) { return values()[(ordinal() + 1) % (supportsSheep ? 4 : 2)]; }
}
