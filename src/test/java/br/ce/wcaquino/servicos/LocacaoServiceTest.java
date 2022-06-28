package br.ce.wcaquino.servicos;

import br.ce.wcaquino.daos.LocacaoDao;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.matchers.DiaSemanaMatcher;
import br.ce.wcaquino.runners.ParallelRunner;
import br.ce.wcaquino.utils.DataUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.LocacaoBuilder.umaLocacao;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.OwnMatchers.*;
import static br.ce.wcaquino.utils.DataUtils.obterData;
import static br.ce.wcaquino.utils.DataUtils.obterDataComDiferencaDias;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(ParallelRunner.class)
public class LocacaoServiceTest {

    @Mock
    private SpcService spcService;
    @Mock
    private EmailService emailService;
    @Mock
    private LocacaoDao dao;
    @InjectMocks
    @Spy
    private LocacaoService service;

    private static int contador;

    @BeforeClass
    public static void setupClass() {
        contador = 0;
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("Foram realizados " + contador + " testes");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

//        dao = mock(LocacaoDao.class);
//        spcService = mock(SpcService.class);
//        emailService = mock(EmailService.class);
//        this.service = new LocacaoService(dao, spcService, emailService);

        contador++;
        System.out.println("Iniciando..." + Thread.currentThread().getName());
    }

    @After
    public void tearDown() {
        System.out.println("Finalizando..." + Thread.currentThread().getName());
    }

    @Test
    public void deveFazerLocarComSucesso_Assume() throws Exception {
        // Assume que só deve executar quando o new Date() nao for Sabado
        Assume.assumeFalse(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));

        //cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilme().comValor(5.0).get();

        //acao
        Locacao locacao = service.alugarFilmes(usuario, List.of(filme1));

        //verificacao via Assert
        Assert.assertEquals(5.0, locacao.getValor(), 0.01);

        Assert.assertTrue(DataUtils.isMesmaData(locacao.getDataLocacao(), new Date()));
        MatcherAssert.assertThat(locacao.getDataLocacao(), ehHoje());

        Assert.assertTrue(DataUtils.isMesmaData(locacao.getDataRetorno(), obterDataComDiferencaDias(1)));
        MatcherAssert.assertThat(locacao.getDataRetorno(), ehHojeComDiferencaDias(1));
    }

    @Test
    public void deveFazerLocarComSucesso_mockandoObterData() throws Exception {
        //cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilme().comValor(5.0).get();

        doReturn(DataUtils.obterData(28, 4, 2017)).when(service).obterData();

        //acao
        Locacao locacao = service.alugarFilmes(usuario, List.of(filme1));

        //verificacao via Assert
        Assert.assertEquals(5.0, locacao.getValor(), 0.01);
        Assert.assertTrue(DataUtils.isMesmaData(locacao.getDataLocacao(), obterData(28, 4, 2017)));
        Assert.assertTrue(DataUtils.isMesmaData(locacao.getDataRetorno(), obterData(29, 4, 2017)));
    }

    @Test(expected = FilmeSemEstoqueException.class)
    public void deveLancarExcecaoAoAlugarFilmeSemEstoque_tratamentoExpectedException() throws Exception {
        //cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilme().get();
        Filme filme2 = umFilmeSemEstoque().get();

        //acao
        service.alugarFilmes(usuario, List.of(filme1, filme2));
    }

    @Test
    public void deveLancarExcecaoAoAlugarFilmeSemEstoque_tratamentoTryCatchComAssertFail() {
        //cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilmeSemEstoque().get();

        //acao
        try {
            service.alugarFilmes(usuario, Collections.singletonList(filme1));
            Assert.fail("Deveria ter lancado excecao!");
        } catch (Exception e) {
            Assert.assertEquals("Filme sem estoque", e.getMessage());
        }
    }

    @Test
    public void deveLancarExcecaoAoAlugarFilmeSemEstoque_tratamentoAssertThrows() {
        //cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilmeSemEstoque().get();

        //acao
        Assert.assertThrows(FilmeSemEstoqueException.class, () -> service.alugarFilmes(usuario, List.of(filme1)));
    }

    @Test
    public void deveLancarExcecaoAoAlugarFilmeSemUsuario_tratamentoTryCatchComAssertFail() throws FilmeSemEstoqueException {
        // cenario
        Filme filme1 = umFilme().get();

        // acao
        try {
            service.alugarFilmes(null, Collections.singletonList(filme1));
            Assert.fail("Deveria lancar excecao de usuario nao informado");
        } catch (LocadoraException e) {
            Assert.assertEquals("Usuario nao informado", e.getMessage());
        }
    }

    @Test
    public void deveLancarExcecaoAoAlugarFilmeNaoInformado_tratamentoTryCatchComAssertFail() {
        // cenario
        Usuario usuario = umUsuario().get();

        // acao
        Assert.assertThrows(LocadoraException.class, () -> service.alugarFilmes(usuario, null));
    }

    @Test
    @Ignore // Ignorado pois o teste foi movido para a classe DDT_CalculoValorLocacaoTest
    public void devePagar75percentoNoFilme3() throws FilmeSemEstoqueException, LocadoraException {
        // cenario
        Usuario usuario = umUsuario().get();

        Filme filme1 = umFilme().get();
        Filme filme2 = umFilme().get();
        Filme filme3 = umFilme().get();

        // acao
        Locacao locacao = this.service.alugarFilmes(usuario, List.of(filme1, filme2, filme3));

        // verificacao
        Assert.assertEquals(11.0, locacao.getValor(), 0.01);
    }

    @Test
    @Ignore // Ignorado pois o teste foi movido para a classe DDT_CalculoValorLocacaoTest
    public void devePagar50percentoNoFilme4() throws FilmeSemEstoqueException, LocadoraException {
        // cenario
        Usuario usuario = umUsuario().get();

        Filme filme1 = umFilme().get();
        Filme filme2 = umFilme().get();
        Filme filme3 = umFilme().get();
        Filme filme4 = umFilme().get();

        // acao
        Locacao locacao = this.service.alugarFilmes(usuario, List.of(filme1, filme2, filme3, filme4));

        // verificacao
        Assert.assertEquals(13.0, locacao.getValor(), 0.01);
    }

    @Test
    @Ignore // Ignorado pois o teste foi movido para a classe DDT_CalculoValorLocacaoTest
    public void devePagar25percentoNoFilme4() throws FilmeSemEstoqueException, LocadoraException {
        // cenario
        Usuario usuario = umUsuario().get();

        Filme filme1 = umFilme().get();
        Filme filme2 = umFilme().get();
        Filme filme3 = umFilme().get();
        Filme filme4 = umFilme().get();
        Filme filme5 = umFilme().get();

        // acao
        Locacao locacao = this.service.alugarFilmes(usuario, List.of(filme1, filme2, filme3, filme4, filme5));

        // verificacao
        Assert.assertEquals(14.0, locacao.getValor(), 0.01);
    }

    @Test
    @Ignore // Ignorado pois o teste foi movido para a classe DDT_CalculoValorLocacaoTest
    public void devePagarZeropercentoNoFilme6() throws FilmeSemEstoqueException, LocadoraException {
        // cenario
        Usuario usuario = umUsuario().get();

        Filme filme1 = umFilme().get();
        Filme filme2 = umFilme().get();
        Filme filme3 = umFilme().get();
        Filme filme4 = umFilme().get();
        Filme filme5 = umFilme().get();
        Filme filme6 = umFilme().get();

        // acao
        Locacao locacao = this.service.alugarFilmes(usuario, List.of(filme1, filme2, filme3, filme4, filme5, filme6));

        // verificacao
        Assert.assertEquals(14.0, locacao.getValor(), 0.01);
    }

    @Test
    public void deveDevolverNaSegundaAoAlugarNoSabado_Assume() throws FilmeSemEstoqueException, LocadoraException {
        // Assume que só deve executar quando o Date() for Sabádo
        Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));

        // cenario
        Usuario usuario = umUsuario().get();

        Filme filme1 = umFilme().get();

        // acao
        Locacao locacao = service.alugarFilmes(usuario, List.of(filme1));

        // verificacao
        boolean ehSegunda = DataUtils.verificarDiaSemana(locacao.getDataRetorno(), Calendar.MONDAY);

        Assert.assertTrue(ehSegunda);

        // Verificacao com Matchers
        MatcherAssert.assertThat(locacao.getDataRetorno(), new DiaSemanaMatcher(Calendar.MONDAY));
        MatcherAssert.assertThat(locacao.getDataRetorno(), caiEm(Calendar.MONDAY));
        MatcherAssert.assertThat(locacao.getDataRetorno(), caiNumaSegundaFeira());
    }

    @Test
    public void deveDevolverNaSegundaAoAlugarNoSabado_MockObterData() throws FilmeSemEstoqueException, LocadoraException {
        // cenario
        Usuario usuario = umUsuario().get();
        Filme filme1 = umFilme().get();

        doReturn(DataUtils.obterData(29, 4, 2017)).when(service).obterData();

        // acao
        Locacao locacao = service.alugarFilmes(usuario, List.of(filme1));

        // verificacao
        boolean ehSegunda = DataUtils.verificarDiaSemana(locacao.getDataRetorno(), Calendar.MONDAY);

        Assert.assertTrue(ehSegunda);

        // Verificacao com Matchers
        MatcherAssert.assertThat(locacao.getDataRetorno(), new DiaSemanaMatcher(Calendar.MONDAY));
        MatcherAssert.assertThat(locacao.getDataRetorno(), caiEm(Calendar.MONDAY));
        MatcherAssert.assertThat(locacao.getDataRetorno(), caiNumaSegundaFeira());
    }

    @Test
    public void deveLancarExcecaoAoAlugarFilmesParaUsuarioNegativado() throws Exception {
        // cenario
        Usuario usuario = umUsuario().get();
        List<Filme> filmes = List.of(umFilme().get());

        // cenario - mock
        when(spcService.possuiNegativacao(any(Usuario.class))).thenReturn(true);

        // acao
        try {
            service.alugarFilmes(usuario, filmes);
            Assert.fail("Deveria ter lancado excecao");
        } catch (Exception e) {
            Assert.assertEquals(LocadoraException.class, e.getClass());
            Assert.assertEquals("Usuario negativado junto ao SPC.", e.getMessage());
        }
    }

    @Test
    public void deveEnviarEmailParaLocacoesAtrasadas() {
        // cenario
        Usuario usuario1 = umUsuario().get();
        Usuario usuario2 = umUsuario().comNome("Usuario em dia").get();
        Usuario usuario3 = umUsuario().comNome("Usuario atrasado 2").get();

        Locacao locacao1 = umaLocacao().comUsuario(usuario1).atrasada().get();
        Locacao locacao2 = umaLocacao().comUsuario(usuario2).get();
        Locacao locacao3 = umaLocacao().comUsuario(usuario3).atrasada().get();
        Locacao locacao4 = umaLocacao().comUsuario(usuario3).atrasada().get();

        List<Locacao> locacoes = List.of(locacao1, locacao2, locacao3, locacao4);

        // cenario - mock
        when(dao.findLocacoesPendentes()).thenReturn(locacoes);

        // acao
        service.notificarAtrasos();

        // verificacao
        verify(emailService, times(3)).notificarAtraso(any(Usuario.class));
        verify(emailService).notificarAtraso(usuario1);
        verify(emailService, never()).notificarAtraso(usuario2);
        verify(emailService, times(2)).notificarAtraso(usuario3);
        verifyNoMoreInteractions(emailService);
        verifyZeroInteractions(spcService);
    }

    @Test
    public void deveTratarErroNoSpc() throws Exception {
        // cenario
        Usuario usuario = umUsuario().get();
        List<Filme> filmes = List.of(umFilme().get());

        when(spcService.possuiNegativacao(usuario)).thenThrow(new Exception("Falha catastrofica"));

        // acao
        try {
            service.alugarFilmes(usuario, filmes);
            Assert.fail("Deveria ter lancado excecao");
        } catch (Exception e) {
            Assert.assertEquals(LocadoraException.class, e.getClass());
            Assert.assertEquals("Problemas com SPC, tente novamente mais tarde.", e.getMessage());
        }
    }

    @Test
    public void deveProrrogarUmLocacao() {
        // cenario
        Locacao locacao = umaLocacao().get();
        int diasProrrogacao = 3;

        // acao
        service.prorrogarLocacao(locacao, diasProrrogacao);

        // verificacao
        ArgumentCaptor<Locacao> argument = ArgumentCaptor.forClass(Locacao.class);
        verify(dao).salvar(argument.capture());
        Locacao locacaoSalva = argument.getValue();

        assertEquals(5.0 * diasProrrogacao, locacaoSalva.getValor(), 0.01);
        MatcherAssert.assertThat(locacaoSalva.getDataLocacao(), ehHoje());
        MatcherAssert.assertThat(locacaoSalva.getDataRetorno(), ehHojeComDiferencaDias(diasProrrogacao));
    }

    @Test
    public void deveCalcularValorLocacao() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // cenario
        List<Filme> filmes = Collections.singletonList(umFilme().get());

        // acao
        Class<LocacaoService> clazz = LocacaoService.class;
        Method calcularValorLocacao = clazz.getDeclaredMethod("calcularValorLocacao", List.class);
        calcularValorLocacao.setAccessible(true);

        Double valor = (Double) calcularValorLocacao.invoke(service, filmes);

        // verificacao
        MatcherAssert.assertThat(valor, Is.is(4.0));
    }
}
