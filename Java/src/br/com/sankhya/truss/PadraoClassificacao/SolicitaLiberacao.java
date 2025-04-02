package br.com.sankhya.dc.PadraoClassificacao;



import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import br.com.sankhya.dctm.helper.EnviaEmailHelper;
import br.com.sankhya.dctm.helper.LiberacaoLimiteHelper;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.ModifingFields;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class SolicitaLiberacao implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent event) throws Exception {}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {}

	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {}

	@Override
	public void beforeCommit(TransactionContext tranCtx) throws Exception {}

	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
		rotina(event,"delete");
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
		rotina(event,"insert");
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
		rotina(event,"update");
	}

	private void rotina(PersistenceEvent event, String quando) throws MGEModelException {
		try {
			DynamicVO vo = (DynamicVO) event.getVo();
			BigDecimal codClt = vo.asBigDecimal("CODCLT");
			
			JapeWrapper daoPadraoClass = JapeFactory.dao(DynamicEntityNames.PADRAO_CLASSIFICACAO);
			JapeWrapper clcDAO = JapeFactory.dao("CaracteristicaAnalisavel");
			JapeWrapper libProdDAO = JapeFactory.dao("AD_LIBPROD");
			DynamicVO registroPadraoClass = daoPadraoClass.findByPK(codClt);
			
			BigDecimal codProd = registroPadraoClass.asBigDecimal("CODPROD");
			String status = registroPadraoClass.asString("AD_STATUS") == null ? "":registroPadraoClass.asString("AD_STATUS");
			String usoProd = getUsoProd(codProd);
			if(!usoProd.equals("P")) {
				return;
			}
			
			/*if(status.equals("2")) {
				throw new MGEModelException("Não é possível alterar. Faça a liberação de limites primeiro.");
			}*/
			
			BigDecimal codClc = vo.asBigDecimal("CODCLC");
			BigDecimal nuChave = new BigDecimal(codClt.toString() + codClc.toString());
			String tabela = "TGACLI";
			BigDecimal evento = new BigDecimal("1005");
			BigDecimal sequencia = buscaSequenciaTSILIB(nuChave,tabela,evento);
			BigDecimal codUsuSolic = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
			Timestamp dataAtual = new Timestamp(System.currentTimeMillis());
			String assunto = "";
			StringBuilder mensagem = new StringBuilder();
			if(quando.equals("insert")) {
				assunto = "Inserido Característica de Classificação " + codClc +" no Padrão " + codClt;
				mensagem.append(assunto);
			}else if (quando.equals("update")){
				assunto = "Alterado Característica de Classificação " + codClc +" no Padrão " + codClt;
				ModifingFields modifingFields = event.getModifingFields();
				Set<Entry<String, Object[]>> entrySet = modifingFields.entrySet();

				for (Entry<String, Object[]> entry : entrySet) {
					String key = entry.getKey();
					mensagem.append("Campo: " + key + "<br>");
					mensagem.append("Valor Anterior: " + modifingFields.getOldValue(key) + "<br>");
					mensagem.append("Valor Novo: " + modifingFields.getNewValue(key) + "<br>");
					mensagem.append("<br>");
		        }
			}else if (quando.equals("delete")) {
				
				
				
				BigDecimal usuarioLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

				JdbcWrapper jdbc = dwfFacade.getJdbcWrapper();
				jdbc.openSession();

				CallableStatement cstmt = jdbc.getConnection().prepareCall("{call AD_STP_INSEREPAD(?, ?, ?, ?, ?, ?, ?)}");
				cstmt.setQueryTimeout(60);
				
				String caracteristica = clcDAO.findByPK(codClc).asString("NOMECLC");
				
				String observacao = "Deletada Característica de Classificação " + codClc +" no Padrão " + codClt + "\n Característica Deletada: " + caracteristica;
				
				
				cstmt.setBigDecimal(1, codProd);
				cstmt.setBigDecimal(2, usuarioLogado);
				cstmt.setBigDecimal(3, sequencia);
				cstmt.setString(4, observacao);
				cstmt.setBigDecimal(5, codClt);
				cstmt.setBigDecimal(6, codClc);
				cstmt.setString(7, nuChave.toString());
				
				cstmt.execute();
				
				throw new Exception ("Uma liberação com o evento 1005 foi solicitada. O item será deletado quando ocorrer a liberação.");
				
				
				
			}
			
			LiberacaoLimiteHelper llh = new LiberacaoLimiteHelper();
			FluidCreateVO lib = llh.criaLiberacaoLimite(nuChave, tabela, evento, sequencia, BigDecimal.ZERO, BigDecimal.ZERO, 
					codUsuSolic, dataAtual, BigDecimal.ZERO, BigDecimal.ONE, assunto, BigDecimal.ZERO,
					null, null, null, null, BigDecimal.ZERO);
			
			llh.insereCampoLiberacaoLimite(lib, "AD_CODPROD", codProd.toString());
			llh.insereCampoLiberacaoLimite(lib, "AD_CODCLT", codClt);
			llh.insereCampoLiberacaoLimite(lib, "AD_CODCLC", codClc);
			
			llh.salvaLiberacaoLimite(lib);
			
			Collection<DynamicVO> users = JapeFactory.dao(DynamicEntityNames.USUARIO).find(" CODUSU IN (SELECT CODUSU FROM TSILIM WHERE EVENTO = ?)",evento);
			for (DynamicVO user : users) {
				BigDecimal codUsu = user.asBigDecimal("CODUSU");
				String email = user.asString("EMAIL");
				
				EnviaEmailHelper eeh = new EnviaEmailHelper();
				eeh.insereEmail(mensagem.toString().toCharArray(), new BigDecimal("3"), assunto, email, codUsu);
			}
			
			JapeWrapper daoLPA = JapeFactory.dao(DynamicEntityNames.PRODUTO_ACABADO);
			Collection<DynamicVO> registrosTPRLPA = daoLPA.find(" CODPRODPA = ? AND IDPROC IN (SELECT PRC.IDPROC FROM TPRPRC PRC WHERE PRC.VERSAO = (SELECT MAX(PRC2.VERSAO) FROM TPRPRC PRC2 WHERE PRC2.CODPRC = PRC.CODPRC))"
					,codProd);
			
			Collection<DynamicVO> libProdVOs = libProdDAO.find("CODPRODPA = ?", codProd);
			
			for(DynamicVO libProdVO : libProdVOs) {
				libProdDAO.prepareToUpdate(libProdVO)
				.set("LIBPADROES", "N")
				.update();
			}
			
			/*for (DynamicVO registroTPRLPA : registrosTPRLPA) {
				daoLPA.prepareToUpdate(registroTPRLPA).set("AD_LIBERADO", "N").update();
			}*/
			
			daoPadraoClass.prepareToUpdate(registroPadraoClass).set("AD_STATUS", "2").update();
			vo.setProperty("AD_BLOQUEADO", "S");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MGEModelException(e.toString());
		}
	}
	
	private String getUsoProd(BigDecimal codProd) throws Exception {
		DynamicVO registro = JapeFactory.dao(DynamicEntityNames.PRODUTO).findByPK(codProd);
		return registro.asString("USOPROD");
	}
	
	private BigDecimal buscaSequenciaTSILIB(BigDecimal nuChave, String tabela, BigDecimal evento) throws Exception {
		Collection<DynamicVO> registro = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE)
				.find(" nuchave = " + nuChave + " and tabela = '" + tabela + "' and evento = " + evento);
		int qtdRegistroInt = registro == null? 0 : registro.size();
		BigDecimal qtdRegistro = new BigDecimal(qtdRegistroInt);
		return qtdRegistro.add(BigDecimal.ONE);
	}
	
}
