package br.com.sankhya.dctm.copiaComposicao.botaoacao;

import java.math.BigDecimal;
import java.util.Collection;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CopiaComposicao implements AcaoRotinaJava {

	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc = null;
	private static Boolean isOpen = Boolean.FALSE;
	private String MsgErro = "";

	BigDecimal usuarioLogado = null;
	BigDecimal idProcDest = null;
	String codProdPaNovo;
	BigDecimal codPrcNovo;
	int codProdPaOrig = 0;
	int idProcOrig = 0;

	public void doAction(ContextoAcao ctx) throws Exception {
		System.out.println("CopiaComposicao.doAction()");
		
		JapeSession.putProperty("br.com.sankhya.truss.copiaKit", true);
		
		if (!ctx.confirmarSimNao("Confirma", "Deseja realizar cópia da composição do produto selecionado?", 1))
			return;

		System.out.println("CopiaComposicao.doAction()");

		for (int i = 0; i < ctx.getLinhas().length; i++) {
			Registro line = ctx.getLinhas()[i];
			try {
				copiarComposicao(ctx, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (MsgErro.equals("")) {

			ctx.setMensagemRetorno("Cópia realizada com sucesso.");
			/* + LancarTela.abrirTela(nufin, LancarTela.Telas.FINANCEIRO, "NUFIN") */

		} else {
			ctx.setMensagemRetorno(MsgErro);
		}
		
	}

	private void copiarComposicao(ContextoAcao ctx, Registro line) throws Exception {

		System.out.println("CopiaComposicao.copiarComposicao()");

		try {
			openSession();
			usuarioLogado = ctx.getUsuarioLogado();

			System.out.println("AQUI:");

			codProdPaNovo = (String) ctx.getParam("CODPRODPANOVO");
			codPrcNovo = BigDecimal.valueOf(((Double) ctx.getParam("CODPRCNOVO") == null) ? 0.0D
					: ((Double) ctx.getParam("CODPRCNOVO")).doubleValue());
			codProdPaOrig = (int) line.getCampo("CODPRODPA");
			idProcOrig = (int) line.getCampo("IDPROC");

			idProcDest = insereTprlpa();
			insereTprlmp();
			// insereTprlpi();

		} catch (Exception e) {
			e.printStackTrace();
			MsgErro = e.getMessage();
		} finally {
			closeSession();
		}
	}

	private BigDecimal insereTprlpa() throws Exception {
		System.out.println("CopiaComposicao.insereTprlpa()");
		JapeWrapper tprprcDAO = JapeFactory.dao(DynamicEntityNames.PROCESSO_PRODUTIVO);
		DynamicVO tprprcVO = tprprcDAO.findOne("this.CODPRC = " + codPrcNovo
				+ "  AND IDPROC = (SELECT MAX(IDPROC) FROM TPRPRC WHERE CODPRC = " + codPrcNovo + ")");
		idProcDest = tprprcVO.asBigDecimal("IDPROC");

		System.out.println("idProcDest no insereTprlpa: " + idProcDest);

		JapeWrapper tprlpaDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO_ACABADO);
		Collection<DynamicVO> tprlpaVO = tprlpaDAO
				.find(" IDPROC = " + this.idProcOrig + " AND CODPRODPA = " + this.codProdPaOrig);

		if (!tprlpaVO.isEmpty()) {
			System.out.println("Entrou no IF do insereTprlpa");

			for (DynamicVO lpaVO : tprlpaVO) {
				System.out.println("Entrou no FOR insereTprlpa");
				tprlpaDAO.create().set("IDPROC", idProcDest).set("CODPRODPA", new BigDecimal(codProdPaNovo))
						.set("CONTROLEPA", " ").set("TAMLOTEPAD", lpaVO.getProperty("TAMLOTEPAD"))
						.set("MULTIDEAL", lpaVO.getProperty("MULTIDEAL"))
						.set("QTDPRODMIN", lpaVO.getProperty("QTDPRODMIN"))
						.set("MULTIDEAL", lpaVO.getProperty("MULTIDEAL"))
						.set("UNTEMPOATRAVESS", lpaVO.getProperty("UNTEMPOATRAVESS"))
						.set("TEMPOATRAVESS", lpaVO.getProperty("TEMPOATRAVESS"))
						.set("BASCALCDTVAL", lpaVO.getProperty("BASCALCDTVAL"))
						.set("TIPOGERASERIE", lpaVO.getProperty("TIPOGERASERIE"))
						.set("MASCSERIE", lpaVO.getProperty("MASCSERIE"))
						.set("TIPOTEMPO", lpaVO.getProperty("TIPOTEMPO"))
						.set("TEMPOFIXO", lpaVO.getProperty("TEMPOFIXO"))
						.set("IDFORMULA", lpaVO.getProperty("IDFORMULA"))
						.set("CODLOCDEST", lpaVO.getProperty("CODLOCDEST"))
						// .set("CODUSUALT", lpaVO.getProperty("TAMLOTEPAD"))
						// .set("DHALTER", lpaVO.getProperty("TAMLOTEPAD"))
						.set("DHCAD", TimeUtils.getNow()).set("CODUSUCAD", usuarioLogado)
						.set("AD_LIBERADO", lpaVO.getProperty("AD_LIBERADO")).save();

			}

		}

		System.out.println("Retorno: " + idProcDest);
		return idProcDest;

	}

	private void insereTprlmp() throws Exception {
		System.out.println("CopiaComposicao.insereTprlmp()");
		BigDecimal idEfx;
		BigDecimal idEfxDest;
		JapeWrapper tpratvDAO = JapeFactory.dao(DynamicEntityNames.ATIVIDADE);
		// DynamicVO tpratvVO = tpratvDAO.findOne("this.IDPROC = " + idProcOrig);
		Collection<DynamicVO> tpratvVO = tpratvDAO.find(" IDPROC = " + idProcOrig); //Busca composição em todas as etapas e insere na primeira que encontrar do processo produtivo destino.

		if (!tpratvVO.isEmpty()) {

			for (DynamicVO atvVO : tpratvVO) {

				idEfx = atvVO.asBigDecimal("IDEFX");

				JapeWrapper tprlmpDAO = JapeFactory.dao(DynamicEntityNames.LISTA_MATERIAIS_ATIVIDADE);
				Collection<DynamicVO> tprlmpVO = tprlmpDAO
						.find(" IDEFX = " + idEfx + " AND CODPRODPA = " + this.codProdPaOrig);

				if (!tprlmpVO.isEmpty()) {
					System.out.println("Entrou no IF insereTprlmp");

					JapeWrapper tpratvDestDAO = JapeFactory.dao(DynamicEntityNames.ATIVIDADE);
					 DynamicVO tpratvDestVO = tpratvDestDAO.findOne("this.IDPROC = " + idProcDest);
					idEfxDest = tpratvDestVO.asBigDecimal("IDEFX");

					for (DynamicVO lmpVO : tprlmpVO) {
						System.out.println("Entrou no FOR insereTprlmp");
						tprlmpDAO.create().set("IDEFX", idEfxDest).set("CODPRODPA", new BigDecimal(codProdPaNovo))
								.set("CONTROLEPA", " ").set("CODPRODMP", lmpVO.getProperty("CODPRODMP"))
								.set("CONTROLEMP", lmpVO.getProperty("CONTROLEMP"))
								.set("CODVOL", lmpVO.getProperty("CODVOL"))
								.set("QTDMISTURA", lmpVO.getProperty("QTDMISTURA"))
								.set("CODLOCALORIG", lmpVO.getProperty("CODLOCALORIG"))
								.set("CODLOCALBAIXA", lmpVO.getProperty("CODLOCALBAIXA"))
								.set("GERAREQUISICAO", lmpVO.getProperty("GERAREQUISICAO"))
								.set("TIPOCONTROLEMP", lmpVO.getProperty("TIPOCONTROLEMP"))
								.set("FIXAQTDAPO", lmpVO.getProperty("FIXAQTDAPO"))
								.set("TIPOQTD", lmpVO.getProperty("TIPOQTD"))
								.set("VERIFICAEST", lmpVO.getProperty("VERIFICAEST"))
								.set("LIBERADESVIO", lmpVO.getProperty("LIBERADESVIO"))
								.set("PERCDESVIOINF", lmpVO.getProperty("PERCDESVIOINF"))
								.set("PERCDESVIOSUP", lmpVO.getProperty("PERCDESVIOSUP"))
								.set("TIPOUSOMP", lmpVO.getProperty("TIPOUSOMP"))
								.set("PROPMPFIXA", lmpVO.getProperty("PROPMPFIXA"))
								// CAMPOS ADICIONAIS ABAIXO:
								.set("AD_FASE", lmpVO.getProperty("AD_FASE"))
								.set("AD_PERCMP", lmpVO.getProperty("AD_PERCMP"))
								.set("AD_PESAGEM", lmpVO.getProperty("AD_PESAGEM"))
								.set("AD_SEQ", lmpVO.getProperty("AD_SEQ")).set("AD_DATAINCLUSAO", TimeUtils.getNow())
								.set("AD_USUARIOINCLUSAO", usuarioLogado).save();
					}
				}
			}
		} else {

			throw new MGEModelException("Produto copiado, porém não contém itens em sua composição.");
			// MsgErro = "Apenas o produto copiado, porém não contém itens em sua
			// composição.";
		}
	}

	private void insereTprlpi() throws Exception {

		JapeWrapper tprlpiDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO_INTERMEDIARIO_PROCESSO);
		Collection<DynamicVO> tprlpiVO = tprlpiDAO
				.find(" IDPROC = " + idProcOrig + " AND CODPRODPA = " + this.codProdPaOrig);

		if (!tprlpiVO.isEmpty()) {
			System.out.println("Entrou no IF insereTprlpi");

			for (DynamicVO lpiVO : tprlpiVO) {
				System.out.println("Entrou no FOR insereTprlpi");
				tprlpiDAO.create().set("IDPROC", idProcDest).set("CODPRODPA", new BigDecimal(codProdPaNovo))
						.set("CONTROLEPA", " ").set("CODPRODPI", lpiVO.getProperty("CODPRODPI"))
						.set("CONTROLEPI", lpiVO.getProperty("CONTROLEPI")).set("TIPOPI", lpiVO.getProperty("TIPOPI"))
						.set("TIPOSUBOP", lpiVO.getProperty("TIPOSUBOP"))
						.set("AGUARDARSUBOP", lpiVO.getProperty("AGUARDARSUBOP"))
						.set("TIPONROLOTE", lpiVO.getProperty("TIPONROLOTE"))
						.set("CONSIDERAQTDEST", lpiVO.getProperty("CONSIDERAQTDEST"))
						.set("REDIMENSIONAPA", lpiVO.getProperty("REDIMENSIONAPA"))
						.set("REDIMENSIONAPAPERDA", lpiVO.getProperty("REDIMENSIONAPAPERDA"))
						.set("CONSIDERALOTEPI", lpiVO.getProperty("CONSIDERALOTEPI"))
						.set("BLOQINITPA", lpiVO.getProperty("BLOQINITPA")).set("CODUSUCAD", usuarioLogado)
						.set("DHCAD", TimeUtils.getNow())
						// .set("DHALTER", lpiVO.getProperty("DHALTER"))
						// .set("CODUSUALT", lpiVO.getProperty("CODUSUALT"))
						.save();
			}
		}

	}

	private static void openSession() {
		try {
			if (isOpen && jdbc != null) {
				return;
			}

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			hnd.setCanTimeout(false);
			jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();
			isOpen = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void closeSession() {
		if (isOpen) {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
			isOpen = false;
			jdbc = null;
		}
	}
}
