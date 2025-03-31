package ru.nsu.palkin;

@MultiInheritance(ClassD.class)
public class ClassB extends ISomeInterfaceRoot {
    @Override
    public void someMethod() {
        System.out.println("This is class B");
    }
}
