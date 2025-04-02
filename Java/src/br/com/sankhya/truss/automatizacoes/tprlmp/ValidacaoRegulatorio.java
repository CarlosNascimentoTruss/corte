package br.com.sankhya.dctm.automatizacoes.tprlmp;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;

import com.sankhya.util.JdbcUtils;
import com.sankhya.util.TimeUtils;

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

public class ValidacaoRegulatorio implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent evt) throws Exception, RuntimeException {
		// TODO Auto-generated method stub
		this.valida(evt, "D");
	}

	@Override
	public void beforeInsert(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		this.valida(evt, "I");
	}

	@Override
	public void beforeUpdate(PersistenceEvent evt) throws Exception {
		// TODO Auto-generated method stub
		this.valida(evt, "U");
	}
	
	private void valida(PersistenceEvent evt, String quando) throws Exception, RuntimeException {
		
		boolean continua = false;
		
		if(JapeSession.getProperties().get("br.com.sankhya.truss.copiaKit") != null) {
			continua = true;
		}
		
		if(JapeSession.getProperties().get("br.com.sankhya.mgeprod.duplicando.processo.produtivo") != null && !continua) {
			return;
		}
		
		
		if(JapeSession.getProperties().get("br.com.mgeprod.isNotValidVersionaProcesso") != null && !continua) {
			return;
		}
		
		DynamicVO newVO = (DynamicVO) evt.getVo();
		
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		JapeWrapper lpaDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO_ACABADO);
		JapeWrapper efxDAO = JapeFactory.dao(DynamicEntityNames.ELEMENTO_FLUXO);
		
		BigDecimal idproc = efxDAO.findByPK(newVO.asBigDecimal("IDEFX")).asBigDecimal("IDPROC");
		String controlePA = newVO.asString("CONTROLEPA");
		BigDecimal codprodpa = newVO.asBigDecimal("CODPRODPA");
		
		BigDecimal codprodmp = newVO.asBigDecimal("CODPRODMP");
		
		DynamicVO paVO = proDAO.findByPK(codprodpa);
		DynamicVO mpVO = proDAO.findByPK(codprodmp);
		
		String usoprodPA = paVO.asString("USOPROD");
		String usoprodMP = mpVO.asString("USOPROD");
		
		
		if((usoprodPA.equals("P") || usoprodPA.equals("2")) && !usoprodMP.equals("M")) {
			return;
		}
		
		if(usoprodPA.equals("V") && !(usoprodMP.equals("M") || usoprodMP.equals("P") || usoprodMP.equals("2"))) {
			return;
		}
		
		BigDecimal usuarioLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
		BigDecimal seqLiberacao = getSeqLiberacao(codprodpa);
		
		
		if(quando.equals("U")) {
			DynamicVO oldVO = (DynamicVO) evt.getOldVO();
			
			if(!newVO.asBigDecimal("CODPRODMP").equals(oldVO.asBigDecimal("CODPRODMP")) || !newVO.asBigDecimal("QTDMISTURA").equals(oldVO.asBigDecimal("QTDMISTURA"))) {
				this.insereTSILIB(codprodpa, usuarioLogado, oldVO.asBigDecimal("CODPRODMP"), newVO.asBigDecimal("CODPRODMP"), seqLiberacao, paVO.asString("DESCRPROD"), idproc, newVO.asBigDecimal("QTDMISTURA"), oldVO.asBigDecimal("QTDMISTURA"), "U");
				
				/*lpaDAO.prepareToUpdateByPK(idproc, codprodpa, controlePA)
				.set("AD_LIBERADO", "N")
				.update();*/
				
				setLiberado(codprodpa, idproc);
				
				newVO.setProperty("AD_BLOQUEADO", "S");
			}
		}
		
		if(quando.equals("I")) {
			this.insereTSILIB(codprodpa, usuarioLogado, newVO.asBigDecimal("CODPRODMP"), newVO.asBigDecimal("CODPRODMP"), seqLiberacao, paVO.asString("DESCRPROD"), idproc, newVO.asBigDecimal("QTDMISTURA"), newVO.asBigDecimal("QTDMISTURA"), "I");
			
			/*lpaDAO.prepareToUpdateByPK(idproc, codprodpa, controlePA)
			.set("AD_LIBERADO", "N")
			.update();*/
			
			setLiberado(codprodpa, idproc);
			
			newVO.setProperty("AD_BLOQUEADO", "S");
		}
		
		if(quando.equals("D")) {
			JapeWrapper procDAO = JapeFactory.dao(DynamicEntityNames.PROCESSO_PRODUTIVO);
			
	        BigDecimal codprc = procDAO.findByPK(idproc).asBigDecimal("CODPRC");
	        BigDecimal versao = procDAO.findByPK(idproc).asBigDecimal("VERSAO");
	        
			String observacao = "Deleção na composição do produto " + codprodpa + " - " + paVO.asString("DESCRPROD") + " | " +
					"Processo: " + codprc + " Versão: " + versao + " | " +
					" MP Removida: " + newVO.asBigDecimal("CODPRODMP") + " | " +
					" Qtd. MP Removida: " + newVO.asBigDecimal("QTDMISTURA") + ".";
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

			JdbcWrapper jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();

			CallableStatement cstmt = jdbc.getConnection().prepareCall("{call AD_STP_INSERETSILIB(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			cstmt.setQueryTimeout(60);
				
			cstmt.setBigDecimal(1, codprodpa);
			cstmt.setBigDecimal(2, usuarioLogado);
			cstmt.setBigDecimal(3, newVO.asBigDecimal("CODPRODMP"));
			cstmt.setBigDecimal(4, newVO.asBigDecimal("CODPRODMP"));
			cstmt.setBigDecimal(5, seqLiberacao);
			cstmt.setString(6, paVO.asString("DESCRPROD"));
			cstmt.setBigDecimal(7, idproc);
			cstmt.setBigDecimal(8, newVO.asBigDecimal("QTDMISTURA"));
			cstmt.setBigDecimal(9, newVO.asBigDecimal("QTDMISTURA"));
			cstmt.setString(10, "D");
			cstmt.setString(11, observacao);
				
			cstmt.execute();
			
			throw new Exception ("Uma liberação com o evento 1002 foi solicitada. O item será deletado quando ocorrer a liberação.");
			

		}
	}
	
	private void setLiberado(BigDecimal codprodpa, BigDecimal idproc) throws Exception {
		JapeWrapper prcDAO = JapeFactory.dao(DynamicEntityNames.PROCESSO_PRODUTIVO);
		JapeWrapper libProdDAO = JapeFactory.dao("AD_LIBPROD");
		
		BigDecimal codprc = prcDAO.findByPK(idproc).asBigDecimal("CODPRC");
		
		DynamicVO libProdVO = libProdDAO.findByPK(codprodpa, codprc);
		
		libProdDAO.prepareToUpdate(libProdVO)
		.set("LIBREGULATORIO", "N")
		.update();
		
	}
	
	private void insereTSILIB(BigDecimal codprodpa, BigDecimal usuarioLogado, BigDecimal codProdMpOld, BigDecimal codProdMp, BigDecimal seqLiberacao, String descrProdPA, BigDecimal idproc, BigDecimal qtdmisturaold, BigDecimal qtdmistura, String tipo) throws Exception {
		
		JapeWrapper libDAO = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE);
        JapeWrapper procDAO = JapeFactory.dao(DynamicEntityNames.PROCESSO_PRODUTIVO);
		
        BigDecimal codprc = procDAO.findByPK(idproc).asBigDecimal("CODPRC");
        BigDecimal versao = procDAO.findByPK(idproc).asBigDecimal("VERSAO");
        String observacao = "";
		
        if(tipo.equals("U")) {
	        observacao = "Alteração na composição do produto " + codprodpa + " - " + descrProdPA + " | " +
								"Processo: " + codprc + " Versão: " + versao + " | " +
								"MP Antiga: " + codProdMpOld + " MP Nova: " + codProdMp + " | " +
								"Qtd. Antiga: " + qtdmistura + " Qtd. Nova: " + qtdmisturaold + ".";
		} else if (tipo.equals("I")) {
			observacao = "Inclusão na composição do produto " + codprodpa + " - " + descrProdPA + " | " +
					"Processo: " + codprc + " Versão: " + versao + " | " +
					" MP Nova: " + codProdMp + " | " +
					" Qtd. Nova: " + qtdmisturaold + ".";
		} else if(tipo.equals("D")) {
			observacao = "Deleção na composição do produto " + codprodpa + " - " + descrProdPA + " | " +
					"Processo: " + codprc + " Versão: " + versao + " | " +
					" MP Removida: " + codProdMp + " | " +
					" Qtd. MP Removida: " + qtdmisturaold + ".";
		}
		
        libDAO.create()
        .set("NUCHAVE", codprodpa)
        .set("TABELA", "TPRLMP")
        .set("EVENTO", BigDecimal.valueOf(1002))
        .set("CODUSUSOLICIT", usuarioLogado)
        .set("DHSOLICIT", TimeUtils.getNow())
        .set("VLRLIMITE", BigDecimal.ONE)
        .set("VLRATUAL", BigDecimal.ONE)
        .set("VLRLIBERADO", BigDecimal.ZERO)
        .set("OBSERVACAO", observacao)
        .set("PERCLIMITE", BigDecimal.ZERO)
        .set("VLRTOTAL", BigDecimal.ZERO)
        .set("PERCANTERIOR", BigDecimal.ZERO)
        .set("VLRANTERIOR", BigDecimal.ZERO)
        .set("SEQUENCIA", seqLiberacao)
        .set("REPROVADO", "N")
        .set("SUPLEMENTO", "N")
        .set("ANTECIPACAO", "N")
        .set("TRANSF", "N")
        .set("CODCENCUS", BigDecimal.valueOf(10201))
        .set("CODTIPOPER", BigDecimal.valueOf(1602))
        .set("ORDEM", BigDecimal.ZERO)
        .set("SEQCASCATA", BigDecimal.ZERO)
        .set("NUCLL", BigDecimal.ZERO)
        .set("AD_CODPROD", codprodpa.toString())
        .set("CODUSULIB", BigDecimal.ZERO)
        .set("AD_CODPRODMPOLD", codProdMpOld.toString())
        .set("AD_CODPRODMPNEW", codProdMp.toString())
        .set("AD_IDPROC", idproc)
        .set("AD_CODPRODPA", codprodpa)
        .save();
        
        
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
	
}
