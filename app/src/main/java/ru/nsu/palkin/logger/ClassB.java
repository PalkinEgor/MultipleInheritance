package ru.nsu.palkin.logger;

import ru.nsu.palkin.OperationInterface;

public class ClassB implements OperationInterface {
    @Override
    public int operation(int a, int b) {
        return a * b;
    }
}
