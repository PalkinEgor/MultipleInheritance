package ru.nsu.palkin.diamond2;

import ru.nsu.palkin.DiamondInterfaceRoot;
import ru.nsu.palkin.MultiInheritance;

@MultiInheritance(ClassD.class)
public class ClassB extends DiamondInterfaceRoot {
    @Override
    public void someMethod() {
        System.out.println("This is class B");
    }
}
