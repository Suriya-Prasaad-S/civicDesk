package com.civicdesk.auth.util.id;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import java.util.Properties;

public class NumericStringSequenceGenerator extends SequenceStyleGenerator {

    @Override
    public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry)
            throws MappingException {
        super.configure(new BasicTypeImpl<>(LongJavaType.INSTANCE, BigIntJdbcType.INSTANCE),
                parameters, serviceRegistry);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return String.valueOf(super.generate(session, object));
    }
}
