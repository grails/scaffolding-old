package org.grails.scaffolding.model

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import spock.lang.Shared
import spock.lang.Specification

class DomainModelServiceSpec extends Specification implements MocksDomain {

    @Shared
    DomainModelServiceImpl domainModelService

    @Shared
    PersistentEntity domainClass

    void setup() {
        domainModelService = new DomainModelServiceImpl()
        domainClass = Mock(PersistentEntity) {
            getJavaClass() >> ScaffoldedDomain
        }
    }

    void "test getEditableProperties valid property"() {
        given:
        PersistentProperty bar = Mock()
        DomainProperty domainProperty = Mock(DomainProperty) {
            1 * getConstraints() >> Mock(ConstrainedProperty) { 1 * isDisplay() >> true }
            1 * getName() >> "bar"
        }
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            1 * build(bar) >> domainProperty
        }
        1 * domainClass.getPersistentProperties() >> [bar]

        when:
        List<DomainProperty> properties = domainModelService.getEditableProperties(domainClass).toList()

        then: "properties that are excluded in the scaffolded property aren't included"
        properties.size() == 1
        properties[0] == domainProperty
    }

    /*
    TODO: Wait until derived is added to next version of GORM
    void "test getEditableProperties derived"() {
        given:
        GrailsDomainClass domainClass = Mock {
            1 * getClazz() >> ScaffoldedDomain
        }
        GrailsDomainClassProperty bar = Mock {
            2 * getName() >> "bar"
            1 * isDerived() >> true
        }
        1 * domainClass.getPersistentProperties() >> [bar]
        domainClass.getConstrainedProperties() >> ["bar": Mock(ConstrainedProperty) { 1 * isDisplay() >> true }]

        when:
        List<GrailsDomainClassProperty> properties = domainModelService.getEditableProperties(domainClass).toList()

        then: "properties that are excluded in the scaffolded property aren't included"
        properties.empty
    }
    */

    void "test getEditableProperties excluded by default"() {
        given:
        PersistentProperty persistentProperty1 = Mock(PersistentProperty)
        PersistentProperty persistentProperty2 = Mock(PersistentProperty)
        PersistentProperty persistentProperty3 = Mock(PersistentProperty)
        DomainProperty dateCreated = Mock(DomainProperty) {
            1 * getName() >> "dateCreated"
        }
        DomainProperty lastUpdated = Mock(DomainProperty) {
            1 * getName() >> "lastUpdated"
        }
        DomainProperty version = Mock(DomainProperty) {
            1 * getName() >> "lastUpdated"
        }
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            1 * build(persistentProperty1) >> dateCreated
            1 * build(persistentProperty2) >> lastUpdated
            1 * build(persistentProperty3) >> version
        }
        1 * domainClass.getPersistentProperties() >> [persistentProperty1, persistentProperty2, persistentProperty3]

        when:
        List<DomainProperty> properties = domainModelService.getEditableProperties(domainClass).toList()

        then: "properties that are excluded by default are excluded"
        properties.empty
    }

    void "test getEditableProperties constraints display false"() {
        given:
        PersistentProperty bar = Mock()
        DomainProperty domainProperty = Mock(DomainProperty) {
            1 * getName() >> "bar"
            1 * getConstraints() >> Mock(ConstrainedProperty) { 1 * isDisplay() >> false }
        }
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            1 * build(bar) >> domainProperty
        }
        1 * domainClass.getPersistentProperties() >> [bar]

        when:
        List<DomainProperty> properties = domainModelService.getEditableProperties(domainClass).toList()

        then: "properties that are excluded in the scaffolded property aren't included"
        properties.empty
    }

    void "test getEditableProperties scaffold exclude"() {
        given:
        PersistentProperty foo = Mock()
        DomainProperty domainProperty = Mock(DomainProperty) {
            1 * getName() >> "foo"
        }
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            1 * build(foo) >> domainProperty
        }
        1 * domainClass.getPersistentProperties() >> [foo]

        when:
        List<DomainProperty> properties = domainModelService.getEditableProperties(domainClass).toList()

        then: "properties that are excluded in the scaffolded property aren't included"
        properties.empty
    }

    void "test hasProperty"() {
        given:
        MappingContext mappingContext = new KeyValueMappingContext("test")
        PersistentEntity persistentEntity = mockDomainClass(mappingContext, ScaffoldedDomain)
        mockDomainClass(mappingContext, EmbeddedAssociate)
        DomainPropertyFactory domainPropertyFactory = mockDomainPropertyFactory(mappingContext)
        domainModelService.domainPropertyFactory = domainPropertyFactory

        expect:
        domainModelService.hasEditableProperty(persistentEntity) { DomainProperty p ->
            p.name == "timeZone"
        }
        domainModelService.hasEditableProperty(persistentEntity) { DomainProperty p ->
            p.name == "locale"
        }
        !domainModelService.hasEditableProperty(persistentEntity) { DomainProperty p ->
            p.name == "not here"
        }
    }

    void "test getVisibleProperties"() {
        given:
        PersistentProperty persistentProperty1 = Mock(PersistentProperty)
        PersistentProperty persistentProperty2 = Mock(PersistentProperty)
        DomainProperty foo = Stub(DomainProperty) {
            getName() >> "foo"
        }
        DomainProperty version = Stub(DomainProperty) {
            getName() >> "version"
        }
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            1 * build(persistentProperty1) >> foo
            1 * build(persistentProperty2) >> version
        }
        1 * domainClass.getPersistentProperties() >> [persistentProperty1, persistentProperty2]

        when:
        List<DomainProperty> properties = domainModelService.getVisibleProperties(domainClass).toList()

        then: "version is excluded"
        properties.size() == 1
        properties[0].name == "foo"
    }

    void "test getShortListVisibleProperties"() {
        given:
        List persistentProperties = (1..10).collect {
            Mock(PersistentProperty)
        }
        List domainProperties = (1..10).collect { num ->
            Stub(DomainProperty) {
                getName() >> num.toString()
            }
        }
        domainProperties.add(Stub(DomainProperty) {
            getName() >> "version"
        })
        PersistentProperty identity = Stub(PersistentProperty)
        domainModelService.domainPropertyFactory = Mock(DomainPropertyFactoryImpl) {
            10 * build(_ as PersistentProperty) >>> domainProperties
            1 * build(identity) >> Stub(DomainProperty) {
                getName() >> "id"
            }
        }
        1 * domainClass.getPersistentProperties() >> persistentProperties
        1 * domainClass.getIdentity() >> identity

        when:
        List<DomainProperty> properties = domainModelService.getShortListVisibleProperties(domainClass).toList()

        then: "Identity is added to the beginning list after trimmed to 6. Version is excluded"
        properties.size() == 7
        properties[0].name == "id"
        properties[6].name == "6"
    }

    class ScaffoldedDomain {
        Long id
        Long version
        static scaffold = [exclude: 'foo']

        EmbeddedAssociate embeddedAssociate
        Locale locale
        byte[] data

        static embedded = ['embeddedAssociate']
    }

    class EmbeddedAssociate {
        Long id
        Long version
        TimeZone timeZone
        Calendar cal
    }
}
