package ru.nsu.palkin.music_example;

import ru.nsu.palkin.DiamondInterfaceRoot;
import ru.nsu.palkin.MultiInheritance;
import ru.nsu.palkin.MusicInterfaceRoot;

@MultiInheritance({Metal.class, Gothic.class})
public class GothicMetal extends MusicInterfaceRoot {

}
