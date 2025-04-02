package br.com.sankhya.dctm.automatizacoes.tprlmp;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import com.sankhya.util.JdbcUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class AutomatizacaoTPRLMP implements EventoProgramavelJava{
	//Evento vinculado a tabela TPRLMP

	@Override
	public void afterDelete(PersistenceEvent event) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
	}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {
	}

	@Override
	public void beforeCommit(TransactionContext tranCtx) throws Exception {
	}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
		valida(event,"D");
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		valida(event,"I");
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		valida(event,"U");
	}
	
	private void valida(PersistenceEvent event, String quando) throws MGEModelException {
		System.out.println("Iniciando AutomatizacaoTPRLMP");
		
		//validaPorcentagem(event);
		//validaPesoLiquido(event);
		
		DynamicVO registroTPRLMP = (DynamicVO) event.getVo();
		
		BigDecimal codProdPa = registroTPRLMP.asBigDecimal("CODPRODPA");
		BigDecimal codProdMp = registroTPRLMP.asBigDecimal("CODPRODMP");
		BigDecimal qtdMisturaNew = BigDecimal.ZERO;
		BigDecimal qtdMisturaOld = BigDecimal.ZERO;
		BigDecimal codProdMpOld = BigDecimal.ZERO;
		
		BigDecimal usuarioLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
		
		registroTPRLMP.setProperty("AD_USUARIOINCLUSAO", usuarioLogado);
		registroTPRLMP.setProperty("AD_DATAINCLUSAO", new Timestamp(System.currentTimeMillis()));
		
		String validaRegulatorio = getValidaRegulatorio(event);
		if(validaRegulatorio.equals( "S")) {
			System.out.println("Entrou no if do valida regulatorio");
			if(quando == "U"){
				DynamicVO registroTPRLMPold = (DynamicVO) event.getOldVO();
				qtdMisturaNew = registroTPRLMP.asBigDecimal("QTDMISTURA");
				qtdMisturaOld = registroTPRLMPold.asBigDecimal("QTDMISTURA");
				BigDecimal codLocalOrigOld = registroTPRLMPold.asBigDecimal("CODLOCALORIG");
				BigDecimal codLocalOrig = registroTPRLMP.asBigDecimal("CODLOCALORIG");
				BigDecimal codLocalBaixaOld = registroTPRLMPold.asBigDecimal("CODLOCALBAIXA");
				BigDecimal codLocalBaixa = registroTPRLMP.asBigDecimal("CODLOCALBAIXA");
				String tipoUsompOld = registroTPRLMPold.asString("TIPOUSOMP");
				String tipoUsomp = registroTPRLMP.asString("TIPOUSOMP");
				BigDecimal codProdPaOld = registroTPRLMPold.asBigDecimal("CODPRODPA");
				String controlePaOld = registroTPRLMPold.asString("CONTROLEPA");
				String controlePa = registroTPRLMP.asString("CONTROLEPA");
				String tipoControleMpOld = registroTPRLMPold.asString("TIPOCONTROLEMP");
				String tipoControleMp = registroTPRLMP.asString("TIPOCONTROLEMP");
				codProdMpOld = registroTPRLMPold.asBigDecimal("CODPRODMP");
				String controleMpOld = registroTPRLMPold.asString("CONTROLEMP");
				String controleMp = registroTPRLMP.asString("CONTROLEMP");
				String tipoQtdOld = registroTPRLMPold.asString("TIPOQTD");
				String tipoQtd = registroTPRLMP.asString("TIPOQTD");
				String codVolOld = registroTPRLMPold.asString("CODVOL");
				String codVol = registroTPRLMP.asString("CODVOL");
				String verificaEstOld = registroTPRLMPold.asString("VERIFICAEST");
				String verificaEst = registroTPRLMP.asString("VERIFICAEST");
				String geraRequisicaoOld = registroTPRLMPold.asString("GERAREQUISICAO");
				String geraRequisicao = registroTPRLMP.asString("GERAREQUISICAO");
				BigDecimal percDesvioInfOld = registroTPRLMPold.asBigDecimal("PERCDESVIOINF");
				BigDecimal percDesvioInf = registroTPRLMP.asBigDecimal("PERCDESVIOINF");
				BigDecimal percDesvioSupOld = registroTPRLMPold.asBigDecimal("PERCDESVIOSUP");
				BigDecimal percDesvioSup = registroTPRLMP.asBigDecimal("PERCDESVIOSUP");
				String fixaQtdApoOld = registroTPRLMPold.asString("FIXAQTDAPO");
				String fixaQtdApo = registroTPRLMP.asString("FIXAQTDAPO");
				String consuRefugoOld = registroTPRLMPold.asString("CONSUREFUGO");
				String consuRefugo = registroTPRLMP.asString("CONSUREFUGO");
				String vinculoSeriePaOld = registroTPRLMPold.asString("VINCULOSERIEPA");
				String vinculoSeriePa = registroTPRLMP.asString("VINCULOSERIEPA");
				String estoqueTerceiroOld = registroTPRLMPold.asString("ESTOQUETERCEIRO");
				String estoqueTerceiro = registroTPRLMP.asString("ESTOQUETERCEIRO");
				String liberaDesvioOld = registroTPRLMPold.asString("LIBERADESVIO");
				String liberaDesvio = registroTPRLMP.asString("LIBERADESVIO");
				String adFaseOld = registroTPRLMPold.asString("AD_FASE");
				String adFase = registroTPRLMP.asString("AD_FASE");
				BigDecimal adPercMpOld = registroTPRLMPold.asBigDecimal("AD_PERCMP");
				BigDecimal adPercMp = registroTPRLMP.asBigDecimal("AD_PERCMP");
				String adSeqOld = registroTPRLMPold.asString("AD_SEQ");
				String adSeq = registroTPRLMP.asString("AD_SEQ");
				String adPesagemOld = registroTPRLMPold.asString("AD_PESAGEM");
				String adPesagem = registroTPRLMP.asString("AD_PESAGEM");
				String propMpFixaOld = registroTPRLMPold.asString("PROPMPFIXA");
				String propMpFixa = registroTPRLMP.asString("PROPMPFIXA");
				if (
					    (codLocalOrigOld == null ? codLocalOrig != null : !codLocalOrigOld.equals(codLocalOrig)) &&
					    (codLocalBaixaOld == null ? codLocalBaixa != null : !codLocalBaixaOld.equals(codLocalBaixa)) &&
					    (tipoUsompOld == null ? tipoUsomp == null : tipoUsompOld.equals(tipoUsomp)) &&
					    (codProdPaOld == null ? codProdPa == null : codProdPaOld.equals(codProdPa)) &&
					    (controlePaOld == null ? controlePa == null : controlePaOld.equals(controlePa)) &&
					    (tipoControleMpOld == null ? tipoControleMp == null : tipoControleMpOld.equals(tipoControleMp)) &&
					    (codProdMpOld == null ? codProdMp == null : codProdMpOld.equals(codProdMp)) &&
					    (controleMpOld == null ? controleMp == null : controleMpOld.equals(controleMp)) &&
					    (tipoQtdOld == null ? tipoQtd == null : tipoQtdOld.equals(tipoQtd)) &&
					    (codVolOld == null ? codVol == null : codVolOld.equals(codVol)) &&
					    (verificaEstOld == null ? verificaEst == null : verificaEstOld.equals(verificaEst)) &&
					    (geraRequisicaoOld == null ? geraRequisicao == null : geraRequisicaoOld.equals(geraRequisicao)) &&
					    (percDesvioInfOld == null ? percDesvioInf == null : percDesvioInfOld.equals(percDesvioInf)) &&
					    (percDesvioSupOld == null ? percDesvioSup == null : percDesvioSupOld.equals(percDesvioSup)) &&
					    (fixaQtdApoOld == null ? fixaQtdApo == null : fixaQtdApoOld.equals(fixaQtdApo)) &&
					    (consuRefugoOld == null ? consuRefugo == null : consuRefugoOld.equals(consuRefugo)) &&
					    (vinculoSeriePaOld == null ? vinculoSeriePa == null : vinculoSeriePaOld.equals(vinculoSeriePa)) &&
					    (estoqueTerceiroOld == null ? estoqueTerceiro == null : estoqueTerceiroOld.equals(estoqueTerceiro)) &&
					    (liberaDesvioOld == null ? liberaDesvio == null : liberaDesvioOld.equals(liberaDesvio)) &&
					    (adFaseOld == null ? adFase == null : adFaseOld.equals(adFase)) &&
					    (adPercMpOld == null ? adPercMp == null : adPercMpOld.equals(adPercMp)) &&
					    (adSeqOld == null ? adSeq == null : adSeqOld.equals(adSeq)) &&
					    (adPesagemOld == null ? adPesagem == null : adPesagemOld.equals(adPesagem)) &&
					    (propMpFixaOld == null ? propMpFixa == null : propMpFixaOld.equals(propMpFixa))
					) {
					    return;
					}
				}
			
				if(verificaGrupo(codProdMp)) {
					return;
				}
				
				BigDecimal seqLiberacao = getSeqLiberacao(codProdPa);
				String descrProdPA = getDescrProd(codProdPa);
				Map propriedades = JapeSession.getProperties();
				
				
				if(quando == "I") {
					Object propriedade = propriedades.get("br.com.sankhya.mgeprod.duplicando.processo.produtivo");
					if(propriedade != null) {
						boolean propriedadeBoolean = (boolean)propriedade;
						if(propriedadeBoolean) {
							return;
						}
					}
				}
				
				Object propriedade = propriedades.get("br.com.mgeprod.isNotValidVersionaProcesso");
				System.out.println("br.com.mgeprod.isNotValidVersionaProcesso : " + propriedade);
				
				boolean propriedadeBoolean = false;
				if(propriedade != null) {
					propriedadeBoolean = (boolean)propriedade;
				}
				
				if (quando == "I" || (quando == "D" && !propriedadeBoolean) || (quando == "U"
						&& (qtdMisturaNew.compareTo(qtdMisturaOld) != 0 || codProdMp.compareTo(codProdMpOld) != 0))) {
					insereTSILIB(codProdPa, usuarioLogado, codProdMpOld, codProdMp, seqLiberacao, descrProdPA);
					updateTPELPA(codProdPa);
				}
		}
	}
	
	private void updateTPELPA(BigDecimal codProdPa) throws MGEModelException {
		JapeWrapper dao = JapeFactory.dao(DynamicEntityNames.PRODUTO_ACABADO);
		try {
			Collection<DynamicVO> registrosTPRLPA = dao.find(" codprodpa = ? and idproc = (select max(idproc) from TPRLPA where codprodpa = ?)",codProdPa,codProdPa);
			for (DynamicVO registroTPRLPA : registrosTPRLPA) {
				dao.prepareToUpdate(registroTPRLPA).set("AD_LIBERADO", "N").update();
			}
		} catch (Exception e) {
			MGEModelException.throwMe(e);
		}
	}
	
	private void insereTSILIB(BigDecimal codProdPa, BigDecimal usuarioLogado, BigDecimal codProdMpOld, BigDecimal codProdMp, BigDecimal seqLiberacao, String descrProdPA) throws MGEModelException {
		JdbcWrapper jdbcWrapper = JapeFactory.getEntityFacade().getJdbcWrapper();;
		NativeSql updtab = new NativeSql(jdbcWrapper);
        updtab.setNamedParameter("CODPRODPA",codProdPa);
        updtab.setNamedParameter("USUARIOLOGADO",usuarioLogado);
        updtab.setNamedParameter("CODPRODMPOLD",codProdMpOld);
        updtab.setNamedParameter("CODPRODMP",codProdMp);
        updtab.setNamedParameter("SEQ",seqLiberacao);
        updtab.setNamedParameter("DESCRPRODPA",descrProdPA);
        try {
			updtab.appendSql("INSERT INTO TSILIB (NUCHAVE, TABELA, EVENTO,CODUSUSOLICIT, DHSOLICIT, VLRLIMITE, VLRATUAL, VLRLIBERADO, OBSERVACAO, PERCLIMITE, VLRTOTAL, PERCANTERIOR, VLRANTERIOR, SEQUENCIA, REPROVADO, SUPLEMENTO, ANTECIPACAO, TRANSF, CODCENCUS, CODTIPOPER, ORDEM, SEQCASCATA, NUCLL, AD_CODPROD, CODUSULIB, AD_CODPRODMPOLD, AD_CODPRODMPNEW)"
					+ "       SELECT LPAD(:CODPRODPA||ROWNUM,10,'0'),'TPRLMP',1002,:USUARIOLOGADO,SYSDATE,1,1,0,'LIBERAÇÃO DE ALTERAÇÃO PSA/PA. PRODUTO: '||TO_CHAR(:CODPRODPA)|| ' - '|| :DESCRPRODPA || '. MP ANTERIOR: '|| :CODPRODMPOLD ||', MP ATUAL: '||:CODPRODMP ,0,0,0,0,:SEQ + ROWNUM,'N','N','N','N','010201',1602,0,0,0,:CODPRODPA, CODUSU, :CODPRODMPOLD, :CODPRODMP FROM TSIUSU"
					+ "       WHERE CODUSU IN (SELECT CODUSU FROM TSILIM WHERE EVENTO = 1002)");
			updtab.executeUpdate();
		} catch (Exception e) {
			MGEModelException.throwMe(e);
		}
	}

	private String getDescrProd(BigDecimal codProdPa) throws MGEModelException {
		String descrProd = "";
		
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("SELECT DESCRPROD  FROM tgfpro"
					+ "    WHERE CODPROD = :CODPRODPA");

			sql.setNamedParameter("CODPRODPA", codProdPa);

			rset = sql.executeQuery();

			if (rset.next()) {
				descrProd = rset.getString("DESCRPROD");
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		return descrProd;
	}

	private BigDecimal getSeqLiberacao(BigDecimal codProdPa) throws MGEModelException {
		BigDecimal seqLiberacao = BigDecimal.ZERO;
		
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("select nvl(MAX(NUCHAVE),0) + 1,nvl(MAX(SEQUENCIA),0) + 1 as SEQ"
					+ "    FROM TSILIB"
					+ "    WHERE AD_CODPROD = :CODPRODPA"
					+ "    AND TABELA = 'TPRLMP' AND EVENTO = 1002");

			sql.setNamedParameter("CODPRODPA", codProdPa);

			rset = sql.executeQuery();

			if (rset.next()) {
				seqLiberacao = rset.getBigDecimal("SEQ");
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		return seqLiberacao;
	}

	private boolean verificaGrupo(BigDecimal codProdMP) throws MGEModelException {
		String usoProd = "";
		BigDecimal grupoProd = BigDecimal.ZERO;
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("SELECT USOPROD, CODGRUPOPROD"
					+ " FROM TGFPRO"
					+ " WHERE CODPROD = :CODPRODMP");

			sql.setNamedParameter("CODPRODMP", codProdMP);

			rset = sql.executeQuery();

			if (rset.next()) {
				usoProd = rset.getString("USOPROD");
				grupoProd = rset.getBigDecimal("CODGRUPOPROD");
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		if(usoProd != null && grupoProd != null) {
			if(usoProd == "P" && grupoProd.compareTo(new BigDecimal("990102000")) == 0) {
				return true;
			}
		}
		return false;
	}

	private String getValidaRegulatorio(PersistenceEvent event) throws MGEModelException {
		DynamicVO registroTPRLMP = (DynamicVO) event.getVo();
		BigDecimal codprodmp = registroTPRLMP.asBigDecimal("CODPRODMP");
		
		String validaRegulatorio = "";
		
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("select distinct nvl(AD_VALIDREGUL,'N') as validaregulatorio"
					+ "	from tgfpro pro"
					+ "	where pro.codprod = :CODPRODMP");

			sql.setNamedParameter("CODPRODMP", codprodmp);

			rset = sql.executeQuery();

			if (rset.next()) {
				validaRegulatorio = rset.getString("validaregulatorio");
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		return validaRegulatorio;
	}

	private void validaPesoLiquido(PersistenceEvent event) throws MGEModelException {
		DynamicVO registroTPRLMP = (DynamicVO) event.getVo();
		BigDecimal codprodpa = registroTPRLMP.asBigDecimal("CODPRODPA");
		BigDecimal pesoliq = BigDecimal.ZERO;
		BigDecimal ad_percmp = registroTPRLMP.asBigDecimal("AD_PERCMP");
		
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("select distinct nvl(pro.pesoliq, 0) as pesoliq"
					+ "   from tgfpro pro"
					+ "  where pro.codprod = :CODPRODPA");

			sql.setNamedParameter("CODPRODPA", codprodpa);

			rset = sql.executeQuery();

			if (rset.next()) {
				pesoliq = rset.getBigDecimal("pesoliq");
				if(pesoliq == null || pesoliq.floatValue() == 0.0) {
					throw new MGEModelException("O peso líquido no cadastro do PA deve estar preenchido. Produto : " + codprodpa);
				}
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		if(pesoliq.floatValue() > 0.0 && ad_percmp != null) {
			registroTPRLMP.setProperty("QTDMISTURA", new BigDecimal((ad_percmp.floatValue() / 100) * pesoliq.floatValue()));
		}
	}

	private void validaPorcentagem(PersistenceEvent event) throws MGEModelException {
		DynamicVO registroTPRLMP = (DynamicVO) event.getVo();
		BigDecimal idefx = registroTPRLMP.asBigDecimal("IDEFX");
		BigDecimal seqmp = registroTPRLMP.asBigDecimal("SEQMP");
		BigDecimal codprodpa = registroTPRLMP.asBigDecimal("CODPRODPA");
		BigDecimal ad_percmp = registroTPRLMP.asBigDecimalOrZero("AD_PERCMP");
		
		BigDecimal percMP = BigDecimal.ZERO;
		
		JdbcWrapper jdbc = null;
		NativeSql sql = null;
		ResultSet rset = null;
		SessionHandle hnd = null;

		try {
			hnd = JapeSession.open();
			hnd.setFindersMaxRows(-1);
			EntityFacade entity = EntityFacadeFactory.getDWFFacade();
			jdbc = entity.getJdbcWrapper();
			jdbc.openSession();

			sql = new NativeSql(jdbc);

			sql.appendSql("select sum(NVL(lmp.ad_percmp,0)) as percmp"
					+ "	from tprlmp lmp"
					+ "	where lmp.idefx = :IDEFX"
					+ "	and lmp.seqmp != :SEQMP"
					+ "	and lmp.codprodpa = :CODPRODPA");

			sql.setNamedParameter("IDEFX", idefx);
			sql.setNamedParameter("SEQMP", seqmp);
			sql.setNamedParameter("CODPRODPA", codprodpa);

			rset = sql.executeQuery();

			if (rset.next()) {
				percMP = rset.getBigDecimal("percmp");
				if((ad_percmp.floatValue() + percMP.floatValue()) > 100) {
					throw new MGEModelException("Soma dos % da grade ultrapassa 100%");
				}
			}

		} catch (Exception e) {
			MGEModelException.throwMe(e);
		} finally {
			JdbcUtils.closeResultSet(rset);
			NativeSql.releaseResources(sql);
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
		
		if(ad_percmp != null) {
			if(ad_percmp.floatValue() == 0.0) {
				throw new MGEModelException("O % desejado deve ser maior do que zero.");
			}
		}
	}
}