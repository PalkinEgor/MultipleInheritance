package ru.nsu.palkin.music_example;

import ru.nsu.palkin.DiamondInterfaceRoot;
import ru.nsu.palkin.MultiInheritance;
import ru.nsu.palkin.MusicInterfaceRoot;

@MultiInheritance({Rock.class, Gothic.class})
public class GothicRock extends MusicInterfaceRoot {

}
