package ru.nsu.palkin.logger;

import java.time.LocalDate;

import ru.nsu.palkin.MultiInheritance;
import ru.nsu.palkin.OperationInterfaceRoot;

@MultiInheritance({ClassA.class, ClassB.class})
public class ABLogger extends OperationInterfaceRoot {
    @Override
    public int operation(int a, int b) {
        LocalDate currentDate = LocalDate.now();
        System.out.println("Current date: " + currentDate);
        return super.operation(a, b);
    }
}
