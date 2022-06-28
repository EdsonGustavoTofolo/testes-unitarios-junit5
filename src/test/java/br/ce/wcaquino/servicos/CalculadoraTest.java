package br.ce.wcaquino.servicos;

import br.ce.wcaquino.runners.ParallelRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ParallelRunner.class)
public class CalculadoraTest {

    private Calculadora calculadora;

    @Before
    public void setup() {
        this.calculadora = new Calculadora();
        System.out.println("Iniciando..." + Thread.currentThread().getName());
    }

    @After
    public void tearDown() {
        System.out.println("Finalizando..." + Thread.currentThread().getName());
    }

    @Test
    public void deveSomarDoisValores() {
        // cenario
        int a = 5;
        int b = 3;

        // acao
        int resultado = calculadora.somar(a, b);

        // verificacao
        Assert.assertEquals(8, resultado);
    }

    @Test
    public void deveSubtrairDoisValores() {
        // cenario
        int a = 8;
        int b = 5;

        // acao
        int resultado = calculadora.subtrair(a, b);

        // verificacao
        Assert.assertEquals(3, resultado);
    }

    @Test
    public void deveDividirDoisValores() {
        // cenario
        int a = 6;
        int b = 3;

        // acao
        int resultado = calculadora.divide(a, b);

        // verificacao
        Assert.assertEquals(2, resultado);
    }

    @Test(expected = ArithmeticException.class)
    public void deveLancarExcecaoAoDividirPorZero() {
        int a = 10;
        int b = 0;

        calculadora.divide(a, b);
    }
}
