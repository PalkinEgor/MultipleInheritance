package ru.nsu.palkin.diamond2;

import ru.nsu.palkin.DiamondInterface;

public class ClassD implements DiamondInterface {

    @Override
    public void someMethod() {
        System.out.println("This is class D");
    }

    @Override
    public void someMethod2() {
        System.out.println("This is class D2");
    }
}
