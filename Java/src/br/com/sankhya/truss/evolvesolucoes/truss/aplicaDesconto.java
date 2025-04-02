/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.FinderWrapper;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
/*     */ import br.com.sankhya.modelcore.dwfdata.vo.ItemNotaVO;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.Timestamp;
/*     */ import java.util.Collection;
/*     */ import java.util.Date;
/*     */ import java.util.Iterator;
/*     */ 
/*     */ public class aplicaDesconto implements AcaoRotinaJava {
/*     */   public void doAction(ContextoAcao ctx) throws Exception {
/*  26 */     Registro[] notas = ctx.getLinhas();
/*  27 */     Object statusNota = null;
/*  28 */     Object statusPed = null;
/*  29 */     Object tipoAnalise = null;
/*  30 */     JdbcWrapper jdbc = null;
/*  31 */     Double perc = (Double)ctx.getParam("PERC");
/*  32 */     String codFab = (String)ctx.getParam("CODFAB");
/*  33 */     String marca = (String)ctx.getParam("MARCA");
/*  34 */     long datahoraEmMillisegundos = (new Date()).getTime();
/*  35 */     Timestamp dtHoraAtual = new Timestamp(datahoraEmMillisegundos);
/*  36 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  37 */     jdbc = dwfEntityFacade.getJdbcWrapper();
/*  38 */     jdbc.openSession();
/*     */     Registro[] array;
/*  40 */     for (int length = (array = notas).length, i = 0; i < length; i++) {
/*  41 */       Registro nota = array[i];
/*  42 */       BigDecimal nuNota = (BigDecimal)nota.getCampo("NUNOTA");
/*  43 */       statusNota = nota.getCampo("STATUSNOTA");
/*  44 */       statusPed = nota.getCampo("AD_STATUSPED");
/*  45 */       NativeSql queNotas = new NativeSql(jdbc);
/*     */       
/*  47 */       queNotas.appendSql("select sum(v_notaduzia) v_notaduzia,\r\n       sum(v_notasemduzia),\r\n       round(((sum(v_notaduzia) / (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n             9) / 100 perc,\r\n       round((sum(vlrunit) *\r\n             (round(((sum(v_notaduzia) /\r\n                     (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n                     9)) / 100),\r\n             9) vlrdecs,\r\n       sum(vlrunit) - round((sum(vlrunit) * (round(((sum(v_notaduzia) /\r\n                                                   (sum(v_notaduzia) +\r\n                                                   sum(v_notasemduzia))) * 100),\r\n                                                   9)) / 100),\r\n                            9) vlrunitnovo\r\n  from (select case\r\n                 when nvl(i.ad_duzia, 'N') = 'S' then\r\n                  i.vlrtot + i.vlripi + i.vlrsubst\r\n                 else\r\n                  0\r\n               end v_notaduzia,\r\n               case\r\n                 when nvl(i.ad_duzia, 'N') != 'S' then\r\n                  i.vlrtot + i.vlripi + i.vlrsubst\r\n                 else\r\n                  0\r\n               end v_notasemduzia,\r\n               i.codprod,\r\n               i.controle,\r\n               i.vlrunit\r\n          from tgfite i, tgfcab c, tgfpro pro\r\n         where i.nunota = c.nunota\r\n           and i.codprod = pro.codprod\r\n           and pro.usoprod != 'M'\r\n           and i.nunota = " + 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/*  80 */           nuNota + ") tab\r\n");
/*     */       
/*  82 */       ResultSet rsPerc = queNotas.executeQuery();
/*  83 */       if (rsPerc.next()) {
/*  84 */         if ("8".equals(statusPed) || "9".equals(statusPed)) {
/*  85 */           ctx.mostraErro("Processo não pode ser realizado, pedido ja faturado ou coletado");
/*     */         } else {
/*     */           
/*  88 */           Collection<?> itens = dwfEntityFacade.findByDynamicFinder(
/*  89 */               new FinderWrapper("ItemNota", "this.NUNOTA = ?", new Object[] { nuNota }));
/*  90 */           for (Iterator<?> ite = itens.iterator(); ite.hasNext(); ) {
/*  91 */             PersistentLocalEntity itemEntity = (PersistentLocalEntity)ite.next();
/*     */ 
/*     */             
/*  94 */             ItemNotaVO itemVO = (ItemNotaVO)((DynamicVO)itemEntity.getValueObject())
/*  95 */               .wrapInterface(ItemNotaVO.class);
/*  96 */             BigDecimal vlrUnit = itemVO.getVLRUNIT()
/*  97 */               .subtract(itemVO.getVLRUNIT().multiply(rsPerc.getBigDecimal("PERC")));
/*  98 */             itemVO.setProperty("VLRUNIT", vlrUnit);
/*  99 */             itemVO.setProperty("VLRTOT", vlrUnit.multiply(itemVO.getQTDNEG()));
/* 100 */             itemVO.setBASEIPI(vlrUnit.multiply(itemVO.getQTDNEG()));
/* 101 */             itemVO.setProperty("AD_VLRIPI", itemVO.getVLRIPI());
/* 102 */             itemVO.setProperty("AD_VLRSUBST", itemVO.getVLRSUBST());
/* 103 */             itemVO.setVLRIPI(itemVO.getALIQIPI().divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP)
/* 104 */                 .multiply(vlrUnit.multiply(itemVO.getQTDNEG())));
/* 105 */             itemVO.setProperty("AD_DHRECALCPRECO", dtHoraAtual);
/* 106 */             itemEntity.setValueObject((EntityVO)itemVO);
/* 107 */             ImpostosHelpper impostoHelper = new ImpostosHelpper();
/* 108 */             impostoHelper.carregarNota(nuNota);
/* 109 */             impostoHelper.calculaICMS(true);
/* 110 */             impostoHelper.totalizarNota(nuNota);
/* 111 */             impostoHelper.salvarNota();
/*     */           } 
/*     */         } 
/*     */       }
/*     */     } 
/* 116 */     jdbc.closeSession();
/* 117 */     ctx.setMensagemRetorno("Desconto aplicado com sucesso!");
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\aplicaDesconto.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */