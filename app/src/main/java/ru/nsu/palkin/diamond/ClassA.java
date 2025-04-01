package ru.nsu.palkin.diamond;

import ru.nsu.palkin.DiamondInterfaceRoot;
import ru.nsu.palkin.MultiInheritance;

@MultiInheritance({ClassD.class})
public class ClassA extends DiamondInterfaceRoot {
    @Override
    public void someMethod() {
        System.out.println("This is class A");
    }

}
