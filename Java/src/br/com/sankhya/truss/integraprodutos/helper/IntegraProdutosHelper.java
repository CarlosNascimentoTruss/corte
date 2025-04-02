package br.com.sankhya.truss.integraprodutos.helper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
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
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;


public class IntegraProdutosHelper {
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	private static String codprodError;
	private static String msgLog = "";
	public static void integraProdutos(String codigoProduto) throws Exception {
		
		try {
			openSession();
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			
			JapeWrapper deParaDAO = JapeFactory.dao("AD_DEPARAUSOPROD");
			JapeWrapper paramDAO = JapeFactory.dao("AD_PARAMIMPPROD");
			JapeWrapper integraDAO = JapeFactory.dao("AD_INTEGRAPRODSAP");
			JapeWrapper outCamposDAO = JapeFactory.dao("AD_OUTROSCAMPOSPARAM");
			JapeWrapper volDAO = JapeFactory.dao(DynamicEntityNames.VOLUME);
			JapeWrapper camDAO = JapeFactory.dao(DynamicEntityNames.CAMPO);
			JapeWrapper gruDAO = JapeFactory.dao(DynamicEntityNames.GRUPO_PRODUTO);
			JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
			
			NativeSql q = new NativeSql(jdbc);
			
			String query = null;
			
			if(codigoProduto == null) {
				query = "SELECT * FROM AD_INTEGRAPRODSAP WHERE NVL(PROCESSADO,'N') = 'N' AND NVL(AJUSTAR,'N') = 'N'";
			} else {
				query = "SELECT * FROM AD_INTEGRAPRODSAP WHERE CODPROD = " + codigoProduto;
			}
			
			ResultSet r = q.executeQuery(query);
			
			String codprodstring = null;
			
			while(r.next()) {
				String codprod = r.getString("CODPROD").replaceAll("[^0-9]", "");
				codprodstring = r.getString("CODPROD");
				
				String processado = "S";
				
				Collection<DynamicVO> newProVOs = new ArrayList<>();
				DynamicVO newProVO = null;
				
				newProVOs = proDAO.find("CODPROD = ?", codprod);
				
				if(newProVOs.size() > 0) {
					newProVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("Produto", new BigDecimal(codprod));
				} else {
					newProVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("Produto");
				}
				
				
				String gravar = "S";
				String ajustar = "N";
				String usoprod = "";
				
				BigDecimal codgrupo = r.getBigDecimal("CODGRUPOPROD");
				String codv = r.getString("CODVOL");
				
				codprodError = codprodstring;
				
				String utilizacaoMaterial = r.getString("UTILIZACAOMATERIAL");
				String tipoMaterial = r.getString("TIPOMATERIAL");
				BigDecimal codgrupoprod = null;
				String codvol = null;
				
				DynamicVO deParaVO = deParaDAO.findOne("NVL(UTILIZACAOMATERIALSAP, 'A') = ? AND NVL(TIPOMATERIALSAP,'A') = ?",
														utilizacaoMaterial,
														tipoMaterial);
				
				if(deParaVO == null) {
					/*
					msgLog = msgLog + "Não foi encontrado mapeamento de Usado Como para os seguintes campos: \n" +
					                "Utilização do Material:  " + utilizacaoMaterial + "\n" +
					                "Tipo do Material:  " + tipoMaterial + "\n" +
					                "Verifique o mapeamento na tela De-Para Usado Como Importação Produtos" + "\n\n";
					
					
					gravar = "N";
					processado = "N";
					*/
					usoprod = "M";
					newProVO.setProperty("USOPROD", "M");
				} else {
					usoprod = deParaVO.asString("USOPROD");
					newProVO.setProperty("USOPROD", usoprod);
				}
				
				DynamicVO paramVO = paramDAO.findByPK(usoprod);
				
				if(paramVO == null) {
					msgLog = msgLog + "Não foi encontrada configuração de parâmetro para o Usado Como: " + usoprod + "\n" +
				             "Verifique a tela de parâmetros: Parâmetros de Importação de Produtos" + "\n\n";
					gravar = "N";
					processado = "N";
				} else if ("S".equals(gravar)){
					newProVO.setProperty("CALCDIFAL", paramVO.asString("CALCDIFAL"));
					newProVO.setProperty("CLASSUBTRIB", paramVO.asBigDecimal("CLASSUBTRIB"));
					newProVO.setProperty("CODCTACTB", paramVO.asBigDecimal("CODCTACTB"));
					newProVO.setProperty("CODENQIPIENT", paramVO.asBigDecimal("CODENQIPIENT"));
					newProVO.setProperty("CODENQIPISAI", paramVO.asBigDecimal("CODENQIPISAI"));
					newProVO.setProperty("CODESPECST", paramVO.asBigDecimal("CODESPECST"));
					newProVO.setProperty("CODEXNCM", paramVO.asBigDecimal("CODEXNCM"));
					newProVO.setProperty("CODIPI", paramVO.asBigDecimal("CODIPI"));
					newProVO.setProperty("CSTIPIENT", paramVO.asBigDecimal("CSTIPIENT"));
					newProVO.setProperty("CSTIPISAI", paramVO.asBigDecimal("CSTIPISAI"));
					newProVO.setProperty("GRUPOCOFINS", paramVO.asString("GRUPOCOFINS"));
					newProVO.setProperty("GRUPOCSSL", paramVO.asString("GRUPOCSSL"));
					newProVO.setProperty("GRUPOICMS", paramVO.asBigDecimal("GRUPOICMS"));
					newProVO.setProperty("GRUPOICMS2", paramVO.asBigDecimal("GRUPOICMS2"));
					newProVO.setProperty("GRUPOPIS", paramVO.asString("GRUPOPIS"));
					newProVO.setProperty("PERCCMTEST", paramVO.asBigDecimal("PERCCMTEST"));
					newProVO.setProperty("PERCCMTFED", paramVO.asBigDecimal("PERCCMTFED"));
					newProVO.setProperty("PERCCMTIMP", paramVO.asBigDecimal("PERCCMTIMP"));
					newProVO.setProperty("PERCCMTNAC", paramVO.asBigDecimal("PERCCMTNAC"));
					newProVO.setProperty("TEMICMS", paramVO.asString("TEMICMS"));
					newProVO.setProperty("TEMIPICOMPRA", paramVO.asString("TEMIPICOMPRA"));
					newProVO.setProperty("TEMIPIVENDA", paramVO.asString("TEMIPIVENDA"));
					newProVO.setProperty("TIPOITEMSPED", paramVO.asString("TIPOITEMSPED"));
					newProVO.setProperty("TIPSUBST", paramVO.asString("TIPSUBST"));
					newProVO.setProperty("AD_PESAGEM", paramVO.asString("AD_PESAGEM"));
					newProVO.setProperty("AD_SEPARACAO", paramVO.asString("AD_SEPARACAO"));
					newProVO.setProperty("CODLOCALPADRAO", paramVO.asBigDecimal("CODLOCALPADRAO"));
					newProVO.setProperty("IDENTIMOB", paramVO.asBigDecimal("IDENTIMOB"));
					newProVO.setProperty("UTILIMOB", paramVO.asBigDecimal("UTILIMOB"));
					newProVO.setProperty("RASTRESTOQUE", paramVO.asString("RASTRESTOQUE"));
					newProVO.setProperty("TEMRASTROLOTE", paramVO.asString("TEMRASTROLOTE"));
					if("S".equals(paramVO.asString("TEMRASTROLOTE"))) {
						newProVO.setProperty("TIPCONTEST", "L");
					}
					
				}
				
				// Localiza De-Para Grupo de Produtos
				//Collection<DynamicVO> gruVOs = gruDAO.find("AD_CODGRUPOPRODSAP = ?", codgrupo);
				
				/*
				if(gruVOs.size() > 1) {
					msgLog = msgLog + "Foi encontrado mais de um grupo de produto possível no mapeamento. " + "\n" +
							"Grupo enviado SAP: " + codgrupo + "\n" +
				            "Verifique o campo 'Grupo de Produtos SAP' no cadastro de Grupo de Produtos. " + "\n\n";
					gravar = "N";
					processado = "N";
				} else if(gruVOs.size() == 0 || gruVOs == null) {
					msgLog = msgLog + "Não foi encontrado grupo de produtos Sankhya correspondente para o grupo de produtos SAP informado "  + "\n" +
							"Grupo enviado SAP: " + codgrupo + "\n" + 
							"Verifique o campo 'Grupo de Produtos SAP' no cadastro de Grupo de Produtos. " + "\n\n";
					gravar = "N";
					processado = "N";
				} else {
					for(DynamicVO gruVO : gruVOs) {
						codgrupoprod = gruVO.asBigDecimal("CODGRUPOPROD");
					}
				}
				*/
				
				codgrupoprod = BigDecimal.ZERO;
				
				// Localiza De-Para Volume
				Collection<DynamicVO> volVOs = volDAO.find("AD_CODVOLSAP = ?", codv);
				
				if(volVOs.size() > 1) {
					msgLog = msgLog + "Foi encontrado mais de um volume possível no mapeamento. " + "\n" +
				             "Volume enviado SAP: " + codv + "\n" +
				             "Verifique o campo 'Volume SAP' no cadastro de Unidades. " + "\n\n";
					gravar = "N";
					processado = "N";
				} else if(volVOs.size() == 0 || volVOs == null) {
					msgLog = msgLog + "Não foi encontrado volume Sankhya correspondente para o volume SAP informado " + "\n" +
							"Volume enviado SAP: " + codv + "\n" +
				             "Verifique o campo 'Volume SAP' no cadastro de Unidades. " + "\n\n";
					gravar = "N";
					processado = "N";
				} else {
					for(DynamicVO volVO : volVOs) {
						codvol = volVO.asString("CODVOL");
					}
				}
				
				// Localiza Campos na aba Outros Campos
				Collection<DynamicVO> outCamposVOs = outCamposDAO.find("USOPROD = ?", usoprod);
				
				for(DynamicVO outCamposVO : outCamposVOs) {
					DynamicVO camVO = camDAO.findOne("NOMETAB = 'TGFPRO' AND NOMECAMPO = ?", outCamposVO.asString("NOMECAMPO"));
					// Se existe o Campo na TDDCAM para a TGFPRO, então pode gravar
					if(camVO != null) {
						if(outCamposVO.asString("TIPO").equals("F")) {
							String tipoCampo = identificaCampo(outCamposVO.asString("NOMECAMPO"));
							
							if(tipoCampo.equals("STRING")) {
								newProVO.setProperty(outCamposVO.asString("NOMECAMPO"), outCamposVO.asString("VALOR"));
							} else if (tipoCampo.equals("BIGDECIMAL")) {
								newProVO.setProperty(outCamposVO.asString("NOMECAMPO"), new BigDecimal(outCamposVO.asString("VALOR")));
							}

						} else {
							
							String tipoCampo = identificaCampo(outCamposVO.asString("NOMECAMPO"));
							
							if(tipoCampo.equals("STRING")) {
								newProVO.setProperty(outCamposVO.asString("NOMECAMPO").replace("\"", ""), r.getString(outCamposVO.asString("VALOR")));
							} else if (tipoCampo.equals("BIGDECIMAL")) {
								newProVO.setProperty(outCamposVO.asString("NOMECAMPO").replace("\"", ""), r.getBigDecimal(outCamposVO.asString("VALOR")));
							}

						}
					}
				}
				
				
				if("S".equals(gravar)) {
					
					
					//Campos preenchidos de forma fixa
					newProVO.setProperty("AD_IMPRIMEETIQUETA", "S");
					newProVO.setProperty("DECQTD", BigDecimal.valueOf(4));
					newProVO.setProperty("DECVLR", BigDecimal.valueOf(4));
					newProVO.setProperty("PRODUTONFE", BigDecimal.ZERO);
					newProVO.setProperty("SOLCOMPRA", "N");
					newProVO.setProperty("TIPLANCNOTA", "Q");
					newProVO.setProperty("USALOCAL", "S");
					newProVO.setProperty("VENCOMPINDIV", "S");
					
					//Campos preenchidos conforme tabela adicional
					newProVO.setProperty("AD_GRAMAMLPMPF", r.getBigDecimal("AD_GRAMAMLPMPF"));
					newProVO.setProperty("AD_PALLET", r.getBigDecimal("AD_PALLET"));
					newProVO.setProperty("ALTURA", r.getBigDecimal("ALTURA"));
					newProVO.setProperty("ATIVO", "N");
					newProVO.setProperty("CODGRUPOPROD", codgrupoprod);
					newProVO.setProperty("CODVOL", codvol);
					newProVO.setProperty("DESCRPROD", r.getString("DESCRPROD"));
					newProVO.setProperty("ESPESSURA", r.getBigDecimal("ESPESSURA"));
					newProVO.setProperty("LARGURA", r.getBigDecimal("LARGURA"));
					newProVO.setProperty("M3", r.getBigDecimal("M3"));
					newProVO.setProperty("NCM", r.getString("NCM") == null ? "00000000" : r.getString("NCM").substring(0,8));
					newProVO.setProperty("ORIGPROD", r.getString("ORIGPROD"));
					newProVO.setProperty("PESOBRUTO", r.getBigDecimal("PESOBRUTO"));
					newProVO.setProperty("PESOLIQ", r.getBigDecimal("PESOLIQ"));
					newProVO.setProperty("QTDEMB", r.getBigDecimal("QTDEMB"));
					newProVO.setProperty("REFERENCIA", r.getString("REFERENCIA"));
					//newProVO.setProperty("TIPGTINNFE", r.getBigDecimal("TIPGTINNFE"));
					newProVO.setProperty("CODPROD", new BigDecimal(codprod));
					
					if(proDAO.findByPK(new BigDecimal(codprod)) == null) {
						try {
							dwfEntityFacade.createEntity(DynamicEntityNames.PRODUTO, (EntityVO) newProVO);
						} catch (Exception e) {
							msgLog = msgLog + "1. Houve um problema ao gravar o produto" + "\n" + e.getMessage();
							processado = "N";
							gravaLog(codprodError, msgLog);
							e.printStackTrace();
						}
						try { 
							new IntegraVoaHelper().integraVoa(codprodstring);
						} catch (Exception e) {
							msgLog = msgLog + "Houve um problema ao gravar unidade alternativas" + "\n" + e.getMessage();
							processado = "N";
							gravaLog(codprodError, msgLog);
						}
					} else {
						try {
							dwfEntityFacade.saveEntity(DynamicEntityNames.PRODUTO, (EntityVO) newProVO);
						} catch (Exception e) {
							msgLog = msgLog + "2. Houve um problema ao gravar o produto" + "\n" + e.getMessage();
							processado = "N";
							gravaLog(codprod, msgLog);
							e.printStackTrace();
						}
						
						
					}
					
					integraDAO.prepareToUpdateByPK(codprodstring)
					.set("PROCESSADO", processado)
					.update();
					
					
				} else {
					gravaLog(codprodstring, msgLog);
				}
			}
		} catch(Exception e) {
			
			msgLog = msgLog + "3. Houve um problema ao gravar o produto" + "\n" + e.getMessage();
			gravaLog(codprodError, msgLog);
			e.printStackTrace();
		} finally {
			msgLog = "";
			codprodError = "";
			
			closeSession();
		}
		
		
	}
	
	private static String identificaCampo(String nomeCampo) throws Exception {
		
		String response = null;
		
		NativeSql query = new NativeSql(jdbc);
		query.setNamedParameter("P_NOMECAMPO", nomeCampo);
		ResultSet r = query.executeQuery("SELECT TIPCAMPO FROM TDDCAM WHERE NOMETAB = 'TGFPRO' AND NOMECAMPO = :P_NOMECAMPO");
		
		while(r.next()) {
			response = r.getString("TIPCAMPO");
			
		}
		
		if(response.equals("I") || response.equals("F")) {
			response = "BIGDECIMAL";
		} else if(response.equals("B") ) {
			response = "BINARIO";
		} else if(response.equals("C") ) {
			response = "CLOB";
		} else if(response.equals("D") || response.equals("H") || response.equals("T") ) {
			response = "DATA";
	    } else if(response.equals("S") ) {
			response = "STRING";
		}
		
		
		
		return response;
		
	}
	
	private static void gravaLog(String codprod, String msg) throws Exception {
		
		JapeWrapper logDAO = JapeFactory.dao("AD_LOGINTEGRAPRODSAP");
		JapeWrapper integraDAO = JapeFactory.dao("AD_INTEGRAPRODSAP");
		
		logDAO.create()
		.set("DHLOG", TimeUtils.getNow())
		.set("CODPROD", codprod)
		.set("DESCRLOG", msg)
		.save();
		
		integraDAO.prepareToUpdateByPK(codprod)
		.set("AJUSTAR", "S")
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