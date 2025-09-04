package io.whyzervellasskx.gifttreasurespro.model.hibernate.entity

import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.LocationRetriever
import io.whyzervellasskx.gifttreasurespro.model.hibernate.HibernateConstants
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@DynamicUpdate
@Entity
@Table(
    name = "mob_data",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_mob_data_name_location", columnNames = ["mob_name", "location"])
    ]
)
class MobData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @Column(name = "mob_name", nullable = false)
    var mobName: String = HibernateConstants.DEFAULT_STRING
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Int = HibernateConstants.DEFAULT_INT
        protected set

    @Basic(optional = true)
    @Column(name = "location", nullable = true)
    var location: LocationRetriever? = null
        protected set

    @Column(name = "storage", nullable = false)
    var storage: Double = HibernateConstants.DEFAULT_DOUBLE
        protected set

    @Column(name = "level", nullable = false)
    var level: Int = 1
        protected set

    constructor()

    constructor(
        mobName: String,
        amount: Int,
        location: LocationRetriever?,
        storage: Double,
        level: Int
    ) {
        this.mobName = mobName
        this.amount = amount
        this.location = location
        this.storage = storage
        this.level = level
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass = if (other is org.hibernate.proxy.HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass = if (this is org.hibernate.proxy.HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as MobData
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        if (this is org.hibernate.proxy.HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
