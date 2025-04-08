package br.com.duziatreze.truss.sankhya;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.dwfdata.vo.ItemNotaVO;
import br.com.sankhya.modelcore.facades.PesquisaCepSPBean;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.ParceiroHellper;
import br.com.sankhya.modelcore.util.PesquisaCepHelper;
import br.com.sankhya.modelcore.util.PesquisaCepHelper.Endereco;

public class aplicaDesconto2 implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		
		//ctx.mostraErro("ENTROU AQUI");

		Registro[] notas = ctx.getLinhas();
		Object statusNota = null;
		Object statusPed = null;
		Object tipoAnalise = null;
		JdbcWrapper jdbc = null;
		
		Double perc = (Double) ctx.getParam("PERC");
		String codFab = (String) ctx.getParam("CODFAB");
		String marca = (String) ctx.getParam("MARCA");

		long datahoraEmMillisegundos = new java.util.Date().getTime();

		Timestamp dtHoraAtual = new Timestamp(datahoraEmMillisegundos);

		final EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();

		for (Registro nota : notas) {

			BigDecimal nuNota = (BigDecimal) nota.getCampo("NUNOTA");

			statusNota = nota.getCampo("STATUSNOTA");
			statusPed = nota.getCampo("AD_STATUSPED");

			
			
			NativeSql queNotas = new NativeSql(jdbc);
			
			

			queNotas.appendSql("select sum(v_notaduzia) v_notaduzia,\r\n" + 
					"       sum(v_notasemduzia),\r\n" + 
					"       round(((sum(v_notaduzia) / (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n" + 
					"             2) / 100 perc,\r\n" + 
					"       round((sum(vlrunit) *\r\n" + 
					"             (round(((sum(v_notaduzia) /\r\n" + 
					"                     (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n" + 
					"                     2)) / 100),\r\n" + 
					"             2) vlrdecs,\r\n" + 
					"       sum(vlrunit) - round((sum(vlrunit) * (round(((sum(v_notaduzia) /\r\n" + 
					"                                                   (sum(v_notaduzia) +\r\n" + 
					"                                                   sum(v_notasemduzia))) * 100),\r\n" + 
					"                                                   2)) / 100),\r\n" + 
					"                            2) vlrunitnovo\r\n" + 
					"  from (select case\r\n" + 
					"                 when nvl(i.ad_duzia, 'N') = 'S' then\r\n" + 
					"                  round((i.vlrtot + i.vlripi + i.vlrsubst),2)\r\n" + 
					"                 else\r\n" + 
					"                  0\r\n" + 
					"               end v_notaduzia,\r\n" + 
					"               case\r\n" + 
					"                 when nvl(i.ad_duzia, 'N') != 'S' then\r\n" + 
					"                  round((i.vlrtot + i.vlripi + i.vlrsubst),2)\r\n" + 
					"                 else\r\n" + 
					"                  0\r\n" + 
					"               end v_notasemduzia,\r\n" + 
					"               i.codprod,\r\n" + 
					"               i.controle,\r\n" + 
					"               i.vlrunit\r\n" + 
					"          from tgfite i, tgfcab c, tgfpro pro\r\n" + 
					"         where i.nunota = c.nunota\r\n" + 
					"           and i.codprod = pro.codprod\r\n" + 
					"           and pro.usoprod != 'M'\r\n" + 
					"           and i.nunota = "+ nuNota +") tab\r\n" );

			final ResultSet rsPerc = queNotas.executeQuery();

			if (rsPerc.next()) {

				if ("8".equals(statusPed)||"9".equals(statusPed)) {
					ctx.mostraErro("Processo n�o pode ser realizado, pedido ja faturado ou coletado");

				} else {

					Collection<?> itens = dwfEntityFacade.findByDynamicFinder(
							new FinderWrapper("ItemNota", "this.NUNOTA = ?", new Object[] { nuNota }));

					for (Iterator<?> ite = itens.iterator(); ite.hasNext();) {
						PersistentLocalEntity itemEntity = (PersistentLocalEntity) ite.next();
						ItemNotaVO itemVO = (ItemNotaVO) ((DynamicVO) itemEntity.getValueObject())
								.wrapInterface(ItemNotaVO.class);

						BigDecimal vlrUnit = itemVO.getVLRUNIT()
								.subtract(itemVO.getVLRUNIT().multiply(rsPerc.getBigDecimal("PERC")));

						//ctx.mostraErro("VLRUNIT"+ vlrUnit);
						
						itemVO.setProperty("VLRUNIT", new BigDecimal(vlrUnit.setScale(2, RoundingMode.HALF_DOWN).doubleValue()));
						itemVO.setProperty("VLRTOT", vlrUnit.multiply(itemVO.getQTDNEG()));
						itemVO.setBASEIPI(vlrUnit.multiply(itemVO.getQTDNEG()));
						itemVO.setProperty("AD_VLRIPI", itemVO.getVLRIPI());
						itemVO.setProperty("AD_VLRSUBST", itemVO.getVLRSUBST());
						itemVO.setVLRIPI(itemVO.getALIQIPI().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
								.multiply(vlrUnit.multiply(itemVO.getQTDNEG())));
						itemVO.setProperty("AD_DHRECALCPRECO", dtHoraAtual);

						itemEntity.setValueObject(itemVO);

						ImpostosHelpper impostoHelper = new ImpostosHelpper();

						impostoHelper.carregarNota(nuNota);
						impostoHelper.calculaICMS(true);
						impostoHelper.totalizarNota(nuNota);
						impostoHelper.salvarNota();
						
					}	


					/*
					 * PersistentLocalEntity cabEntity = dwfEntityFacade
					 * .findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] {
					 * nuNota }); DynamicVO cabVO = (DynamicVO) cabEntity.getValueObject();
					 * 
					 * cabVO.setProperty("AD_DHRECALCPRECO", dtHoraAtual);
					 * cabEntity.setValueObject((EntityVO) cabVO);
					 * 
					 * CentralFinanceiro financeiro = new CentralFinanceiro();
					 * financeiro.inicializaNota(nuNota); financeiro.refazerFinanceiro();
					 */

				}

			}

			/*
			 * JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central",
			 * (Object)Boolean.TRUE);
			 * 
			 * 
			 * CentralFinanceiro financeiro = new CentralFinanceiro();
			 * financeiro.inicializaNota(nuNota); financeiro.refazerFinanceiro();
			 */
			
			jdbc.closeSession();

			ctx.setMensagemRetorno("Desconto aplicado com sucesso!");
		}

		

	}

}
