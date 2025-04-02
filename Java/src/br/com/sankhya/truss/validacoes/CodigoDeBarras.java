package br.com.sankhya.dc.validacoes;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.MGEModelException;
import java.math.BigDecimal;
import java.util.Collection;

public class CodigoDeBarras implements EventoProgramavelJava {
  public void afterDelete(PersistenceEvent arg0) throws Exception {}
  
  public void afterInsert(PersistenceEvent arg0) throws Exception {}
  
  public void afterUpdate(PersistenceEvent arg0) throws Exception {}
  
  public void beforeCommit(TransactionContext arg0) throws Exception {}
  
  public void beforeDelete(PersistenceEvent arg0) throws Exception {}
  
  public void beforeInsert(PersistenceEvent arg0) throws Exception {
    String tabela = arg0.getEntity().getName();
    DynamicVO registro = (DynamicVO)arg0.getVo();
    String codBarras = "";
    String controle = "";
    BigDecimal codProd = null;
    BigDecimal nunota = null;
    String ativo = "";
    BigDecimal codEmp = null;
    String str1;
    switch ((str1 = tabela).hashCode()) {
      case 394036867:
        if (!str1.equals("AD_TRASITEMCONF"))
          break; 
        codBarras = registro.asString("CODBARRA");
        controle = registro.asString("CONTROLE");
        codProd = registro.asBigDecimal("CODPROD");
        nunota = registro.asBigDecimal("NUNOTA");
        codEmp = JapeFactory.dao("CabecalhoNota").findByPK(new Object[] { nunota }).asBigDecimal("CODEMP");
        break;
      case 1652305832:
        if (!str1.equals("AD_TPRAMPPARCIAL"))
          break; 
        codBarras = registro.asString("CODBARRAS");
        controle = registro.asString("CONTROLEMP");
        codProd = registro.asBigDecimal("CODPRODMP");
        codEmp = new BigDecimal("6");
        break;
      case 2004034224:
        if (!str1.equals("AD_SEPPARCIAL"))
          break; 
        codBarras = registro.asString("CODBARRAS");
        controle = registro.asString("CONTROLE");
        codProd = registro.asBigDecimal("CODPROD");
        nunota = registro.asBigDecimal("NUNOTA");
        codEmp = JapeFactory.dao("CabecalhoNota").findByPK(new Object[] { nunota }).asBigDecimal("CODEMP");
        break;
    } 
    Collection<DynamicVO> registrosEtiqueta = JapeFactory.dao("AD_TRASETIQUETA").find(" CODBARRAS = '" + codBarras + 
        "' AND CONTROLE = '" + controle + "' AND CODPROD = " + codProd + " AND CODEMP = " + codEmp);
    if (registrosEtiqueta.size() == 0)
      throw new MGEModelException("Nfoi encontrado registro de ETIQUETA." + 
          System.lineSeparator() + "Produto: " + codProd + 
          System.lineSeparator() + "Controle: " + controle + 
          System.lineSeparator() + "Cde Barras: " + codBarras + 
          System.lineSeparator() + "Empresa: " + codEmp); 
    DynamicVO registroEtiqueta = registrosEtiqueta.iterator().next();
    controle = registroEtiqueta.asString("CONTROLE");
    codProd = registroEtiqueta.asBigDecimal("CODPROD");
    BigDecimal codLocal = registroEtiqueta.asBigDecimal("CODLOCAL");
    
    if(codLocal == null) {
    	 throw new MGEModelException("Local não está cadastrado na etiqueta. Verifique com o setor responsável.");
    }
    
    DynamicVO registroEstoque = JapeFactory.dao("Estoque")
      .findByPK(new Object[] { codEmp, codProd, codLocal, controle, BigDecimal.ZERO, "P" });
    if (registroEstoque == null)
      throw new MGEModelException("Nfoi encontrado registro de ESTOQUE." + 
          System.lineSeparator() + "Produto: " + codProd + 
          System.lineSeparator() + "Controle: " + controle + 
          System.lineSeparator() + "Cde Barras: " + codBarras + 
          System.lineSeparator() + "Empresa: " + codEmp); 
    ativo = registroEstoque.asString("AD_ATIVOLOTE");
    if (ativo == null || "N".equals(ativo))
      throw new MGEModelException("Lote Inativo, procurar setor da Qualidade"); 
  }
  
  public void beforeUpdate(PersistenceEvent arg0) throws Exception {}
}
