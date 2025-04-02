package br.com.sankhya.truss.integraprodutos.helper;



import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;


public class IntegraVoaHelper {
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	private static String codprodError;
	private static String codvolError;
	private static String msgLog = "";
	public static void integraVoa(String codigoProduto) throws Exception {
		
		try {
			openSession();
			
			JapeWrapper integraDAO = JapeFactory.dao("AD_INTEGRATGFVOA");
			JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
			JapeWrapper voaDAO = JapeFactory.dao(DynamicEntityNames.VOLUME_ALTERNATIVO);
			JapeWrapper volDAO = JapeFactory.dao(DynamicEntityNames.VOLUME);
			
			NativeSql q = new NativeSql(jdbc);
			
			String query = null;
			
			if(codigoProduto == null) {
				query = "SELECT * FROM AD_INTEGRATGFVOA WHERE NVL(PROCESSADO,'N') = 'N' AND NVL(AJUSTAR,'N') = 'N'";
			} else {
				query = "SELECT * FROM AD_INTEGRATGFVOA WHERE CODPROD = " + codigoProduto;
			}
			
			ResultSet r = q.executeQuery(query);
			
			while(r.next()) {
				
				String codprodstring = r.getString("CODPROD");
				String codvolalt = r.getString("CODVOLALT");
				String quantidade = r.getString("QUANTIDADE");
				String eangtin = r.getString("EANGTIN");
				String gravar = "S";
				String codvol = null;
				
				codprodError = codprodstring;
				codvolError = codvolalt;
				
				if(codprodstring == null) {
					msgLog = msgLog + "Informação do produto não preenchido";
					gravar = "N";
					
				}
				if(codvolalt == null) {
					msgLog = msgLog + "Informação da unidade alternativa não preenchido";
					gravar = "N";
					
				}
				if(quantidade == null) {
					msgLog = msgLog + "Informação de quantidade não preenchido";
					gravar = "N";
					
				}
				
				DynamicVO volVO = volDAO.findOne("AD_CODVOLSAP = ?", codvolalt);
				
				if(volVO == null) {
					msgLog = msgLog + "De-Para de unidade não localizado no cadastro.";
					gravar = "N";
				} else {
					codvol = volVO.asString("CODVOL");
				}
				
				BigDecimal quantidadeInt = null;
				
				try {
					quantidadeInt = new BigDecimal(quantidade);
				} catch(Exception e) {
					msgLog = msgLog + "Campo quantidade é um número inválido";
					gravar = "N";
					
				}
				
				BigDecimal codprod = new BigDecimal(codprodstring.replaceAll("[^0-9]", ""));
				
				DynamicVO proVO = proDAO.findByPK(codprod);
				
				if(proVO == null) {
					msgLog = msgLog + "Produto informado não existe no cadastro";
					gravar = "N";
					
				} else {
					try {
						
						if(gravar.equals("S")) {
							voaDAO.create()
							.set("CODPROD", codprod)
							.set("CODVOL", codvol)
							.set("DIVIDEMULTIPLICA", "M")
							.set("QUANTIDADE", quantidadeInt)
							.set("CODBARRA", eangtin)
							.save();
						}
					} catch(Exception e) {
						msgLog = msgLog + "Problema ao cadastrar unidade alternativa: \n" + e.getMessage();
						gravar = "N";
					}

				}
				
				if("N".equals(gravar)) {
					gravaLog(codprodstring, codvolalt, msgLog);
				}
				
				
				if(gravar.equals("S")) {
					integraDAO.prepareToUpdateByPK(codprod, codvolalt)
					.set("PROCESSADO", "S")
					.update();
				}
				
			}
		} catch(Exception e) {
			
			msgLog = msgLog + "Houve um problema ao gravar a unidade alternativa" + "\n" + e.getMessage();
			gravaLog(codprodError, codvolError, msgLog);
			e.printStackTrace();
		} finally {
			msgLog = "";
			codprodError = "";
			closeSession();
		}
	}
	
	private static void gravaLog(String codprod, String codvol, String msg) throws Exception {
		
		JapeWrapper logDAO = JapeFactory.dao("AD_LOGINTEGRAVOASAP");
		JapeWrapper integraDAO = JapeFactory.dao("AD_INTEGRATGFVOA");
		
		logDAO.create()
		.set("DHLOG", TimeUtils.getNow())
		.set("CODPROD", codprod)
		.set("CODVOLALT", codvol)
		.set("DESCRLOG", msg)
		.save();
		
		integraDAO.prepareToUpdateByPK(codprod, codvol)
		.set("AJUSTAR", "S")
		.set("PROCESSADO", "N")
		.update();
		
		msgLog = "";
	}
	
	public static void replicaCampos(ContextoAcao ctx) throws Exception {
		
		try {
			openSession();
			
			JapeWrapper paramDAO = JapeFactory.dao("AD_PARAMIMPPROD");
			JapeWrapper outCamposDAO = JapeFactory.dao("AD_OUTROSCAMPOSPARAM");
			
			Registro[] linhasSelecionadas = ctx.getLinhas();
			
			for(Registro linha : linhasSelecionadas) {
				Collection<DynamicVO> paramVOs = paramDAO.find("USOPROD <> ?", (String) linha.getCampo("USOPROD"));
				
				for(DynamicVO paramVO : paramVOs) {
					DynamicVO outCampoVO = outCamposDAO.findByPK(paramVO.asString("USOPROD"), (String) linha.getCampo("NOMECAMPO"));
					
					if(outCampoVO == null) {
						outCamposDAO.create()
						.set("USOPROD", paramVO.asString("USOPROD"))
						.set("NOMECAMPO", (String) linha.getCampo("NOMECAMPO"))
						.set("TIPO", (String) linha.getCampo("TIPO"))
						.set("VALOR", (String) linha.getCampo("VALOR"))
						.save();
					}
				}	
			}
			
			ctx.setMensagemRetorno("Itens Replicados com Sucesso.");
			
		} catch (Exception e) {
			e.printStackTrace();
			ctx.mostraErro(e.getMessage());
		} finally {
			closeSession();
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
		if(isOpen) {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
			isOpen = false;
			jdbc = null;
		}
	}
}