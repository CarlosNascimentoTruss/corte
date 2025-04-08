package br.com.sankhya.truss.corte.actions;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom.Element;

import com.sankhya.util.TimeUtils;
import com.sankhya.util.XMLUtils;

import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;
import br.com.sankhya.truss.corte.scheduled.CorteLocal;
import br.com.sankhya.ws.ServiceContext;

public class CorteGlobal implements AcaoRotinaJava {
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		JapeWrapper topDAO = JapeFactory.dao(DynamicEntityNames.TIPO_OPERACAO);
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc  = dwfEntityFacade.getJdbcWrapper();
		
		try {
			//openSession();
			
			Registro[] linhas = ctx.getLinhas();
			String msgretorno = "";
			
			JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
			JapeSessionContext.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
			ServiceContext sctx = new ServiceContext(null);
		    sctx.setAutentication(AuthenticationInfo.getCurrent());
		    sctx.makeCurrent();
			
			
			for(Registro linha : linhas) {
				BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
				
				DynamicVO cabVO = cabDAO.findByPK(nunota);
				
				if(!cabVO.asString("AD_STATUSPED").equals("4")) {
					ctx.mostraErro("O Pedido deve estar com status Pedido Aprovado.");
				}
				
				Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", nunota);
				int qtdItens = itesVO.size();
				int countDel = 0;
				
				for(DynamicVO iteVO : itesVO) {
					BigDecimal disponivel = BigDecimal.ZERO;
					BigDecimal codprod = iteVO.asBigDecimal("CODPROD");
					BigDecimal codlocal = iteVO.asBigDecimal("CODLOCALORIG");
					BigDecimal qtdneg = iteVO.asBigDecimal("QTDNEG");
					BigDecimal qtdMinVenda = BigDecimal.ZERO;
					BigDecimal sequencia = iteVO.asBigDecimal("SEQUENCIA");
					NativeSql query = new NativeSql(jdbc);
					query.setNamedParameter("P_CODPROD", codprod);
					query.setNamedParameter("P_CODLOCAL", codlocal);
					
					//ctx.mostraErro("CODPROD: " + codprod + "CODLOCAL: " + codlocal);
					
					ResultSet r = query.executeQuery("SELECT DISPONIVEL FROM AD_VW_ESTOQUEGLOBAL WHERE CODPROD = :P_CODPROD AND CODLOCAL = :P_CODLOCAL");
					
					if(r.next()) {
						disponivel = r.getBigDecimal("DISPONIVEL");
					}
					
					
					
					
					if(disponivel.compareTo(qtdneg) < 0) {
						
						DynamicVO proVO = proDAO.findByPK(codprod);
						qtdMinVenda = proVO.asBigDecimal("AD_QTDMINVENDA");
						if(qtdMinVenda == null) {
							throw new Exception("Produto " + codprod + " - " + proVO.asString("DESCRPROD") + " não possui cadastro de quantidade mínima para venda.\nRealize o cadastro.");
						}
						
						BigDecimal newQtdNeg = disponivel.subtract(disponivel.remainder(qtdMinVenda));
						
						iteDAO.prepareToUpdateByPK(nunota, sequencia)
						.set("AD_CLASSCORT", "RP")
						.update();
						
						
						if(newQtdNeg.signum() <= 0) {
							try {
								countDel++;
								iteDAO.prepareToUpdateByPK(nunota, sequencia)
								.set("AD_CLASSCORT", "RT")
								.update();
								
								iteDAO.delete(new Object[] { nunota, sequencia });
							} catch (Exception e) {
								ctx.mostraErro("Erro ao deletar\n" + e.getMessage());
							}
						} else {
							
							iteVO.setProperty("QTDNEG", newQtdNeg);
							iteVO.setProperty("VLRTOT", newQtdNeg.multiply(iteVO.asBigDecimal("VLRUNIT")));
							
							CentralItemNota itemNota = new CentralItemNota();
							itemNota.recalcularValores("QTDNEG", newQtdNeg.toString(), iteVO, nunota);
							
							
							List<DynamicVO> itensVO = new ArrayList<DynamicVO>();
							itensVO.add(iteVO);
							
							CACHelper cacHelper = new CACHelper();
							cacHelper.incluirAlterarItem(nunota, sctx, null, true, itensVO);
							
							iteVO.setProperty("AD_CLASSCORT", null);
							dwfEntityFacade.saveEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) iteVO);
						}
					}
					
					
				}
				
				// Atualiza a TOP de Corte
				BigDecimal topCorte = CorteHelper.buscaTopCorte(cabVO, jdbc);
				Timestamp dhTopCorte = null;
				NativeSql sql = new NativeSql(jdbc);
				sql.setNamedParameter("P_CODTIPOPER", topCorte);
				ResultSet rs = sql.executeQuery("SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = :P_CODTIPOPER");
				if(rs.next()) {
					dhTopCorte = rs.getTimestamp("DHALTER");
				}
				
				
				cabDAO.prepareToUpdate(cabVO)
				.set("CODTIPOPER", topCorte)
				.set("DHTIPOPER", dhTopCorte)
				.update();
				
				
				
				// Atualiza itens para reservar
				NativeSql q = new NativeSql(jdbc);
				q.setNamedParameter("P_NUNOTA", nunota);
				ResultSet r = q.executeQuery("SELECT NUNOTA, SEQUENCIA FROM TGFITE WHERE NUNOTA = :P_NUNOTA");
				
				while(r.next()) {
					iteDAO.prepareToUpdateByPK(r.getBigDecimal("NUNOTA"), r.getBigDecimal("SEQUENCIA"))
					.set("ATUALESTOQUE", BigDecimal.ONE)
					.set("RESERVA", "S")
					.update();
				}
				
				
				if (countDel == qtdItens) {
					cabDAO.prepareToUpdateByPK(nunota)
					.set("AD_CLASSCORTE","RT")
					.update();
					
					cabDAO.deleteByCriteria("NUNOTA = ?", nunota);
				} else {
				
					CorteLocal corte = new CorteLocal();
					String statusPed = corte.executaCorteLocal(nunota);
					
					if(statusPed.equals("-1")) {
						ctx.mostraErro("Erro ao executar Corte Local");
					} else {
						cabDAO.prepareToUpdate(cabVO)
						.set("AD_STATUSPED", statusPed)
						.update();
						
					}
				}
				
				cabDAO.prepareToUpdate(cabVO)
				.set("AD_DTLIBEXP", TimeUtils.getNow())
				.update();
				
			}
			
			ctx.setMensagemRetorno("Corte Executado com Sucesso.");
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Erro ao executar a ação. " + e.getMessage());
		}
	}
	
	private static void alterarItem(DynamicVO iteVO, BigDecimal qtdneg) throws Exception {
		try {	
				CACSP cacSP = (CACSP) ServiceUtils.getStatelessFacade(CACSPHome.JNDI_NAME, CACSPHome.class);
				ServiceContext ctx = new ServiceContext(null);
			    ctx.setAutentication(AuthenticationInfo.getCurrent());
			    ctx.makeCurrent();
				
				Element requestBody = ctx.getRequestBody();
				Element responseBody = ctx.getBodyElement();
				
				responseBody.removeContent();
				requestBody.removeContent();
				
				Element notaElem = new Element("nota");
				XMLUtils.addAttributeElement(notaElem, "NUNOTA", iteVO.asBigDecimal("NUNOTA"));
				
				Element itensElem = new Element("itens");
				Element itemElem = new Element("item");
				XMLUtils.addContentElement(itemElem, "NUNOTA", iteVO.asBigDecimal("NUNOTA"));
				XMLUtils.addContentElement(itemElem, "SEQUENCIA", iteVO.asBigDecimal("SEQUENCIA"));
				XMLUtils.addContentElement(itemElem, "CODPROD", iteVO.asBigDecimal("CODPROD"));
				XMLUtils.addContentElement(itemElem, "QTDNEG", qtdneg);
				XMLUtils.addContentElement(itemElem, "CONTROLE", iteVO.asString("CONTROLE"));
				XMLUtils.addContentElement(itemElem, "VLRUNIT", iteVO.asBigDecimal("VLRUNIT"));
				XMLUtils.addContentElement(itemElem, "VLRTOT", iteVO.asBigDecimal("VLRUNIT").multiply(qtdneg));
				XMLUtils.addContentElement(itemElem, "CODLOCALORIG", iteVO.asBigDecimal("CODLOCALORIG"));
				XMLUtils.addContentElement(itemElem, "CODVOL", iteVO.asString("CODVOL"));
				XMLUtils.addContentElement(itemElem, "PERCDESC", iteVO.asBigDecimal("PERCDESC"));
				XMLUtils.addContentElement(itemElem, "CALCULARDESCONTO", "S");
				XMLUtils.addContentElement(itemElem, "IGNORARRECALCDESC", "N");
				
				
				itensElem.addContent(itemElem);
				notaElem.addContent(itensElem);
				requestBody.addContent(notaElem.detach());
				
				cacSP.incluirAlterarItemNota(ctx);
			} catch (Exception e){
				e.printStackTrace();
				throw new Exception("Erro ao editar item do pedido.\n" + e.getMessage());
			}
		}
	
	public static void recalculaNota(BigDecimal nunota) throws Exception {
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		DynamicVO cabVO = cabDAO.findByPK(nunota);
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nunota});
		
		ImpostosHelpper imposto = new ImpostosHelpper();
		imposto.carregarNota(nunota);
		imposto.calculaICMS(true);
		
		imposto.totalizarNota(nunota);
		imposto.setForcarRecalculo(true);
		imposto.setAtualizaImpostos(true);
		imposto.setCalcularTudo(true);
		imposto.calcularImpostos(nunota);
		imposto.salvarNota();
		
        BigDecimal totalNota = imposto.calcularTotalNota(cabVO.asBigDecimal("NUNOTA"), imposto.calcularTotalItens(cabVO.asBigDecimal("NUNOTA"), false));
		
        cabVO.setProperty("VLRNOTA", totalNota);
        persistentEntityCab.setValueObject((EntityVO)cabVO);	
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
