package br.com.sankhya.truss.corte.depara.util;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.VOProperty;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;

import java.util.Iterator;

public class Duplicate {
    public static FluidCreateVO duplicate(JapeWrapper dao, DynamicVO modeloVO) throws Exception {
        FluidCreateVO fluidCreateVO = dao.create();
        Iterator<VOProperty> iterator = modeloVO.iterator();
        while (iterator.hasNext()) {
            VOProperty property = iterator.next();
            fluidCreateVO.set(property.getName(), property.getValue());
        }
        return fluidCreateVO;
    }

    public static DynamicVO save(FluidCreateVO fluidCreateVO) throws Exception {
        return fluidCreateVO.save();
    }

    public static FluidCreateVO alterProp(FluidCreateVO fluidCreateVO, String prop, Object value) {
        fluidCreateVO.set(prop,value);
        return fluidCreateVO;
    }
}
