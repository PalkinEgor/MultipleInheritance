package ru.nsu.palkin.diamond;

import ru.nsu.palkin.DiamondInterfaceRoot;
import ru.nsu.palkin.MultiInheritance;

@MultiInheritance({ClassA.class, ClassB.class})
public class ClassC extends DiamondInterfaceRoot {
    @Override
    public void someMethod() {
        System.out.println("This is class C");
    }
}
