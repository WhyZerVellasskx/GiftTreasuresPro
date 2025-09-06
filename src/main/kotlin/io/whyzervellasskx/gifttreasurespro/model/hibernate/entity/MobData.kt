package io.whyzervellasskx.gifttreasurespro.model.hibernate.entity

import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.LocationRetriever
import io.whyzervellasskx.gifttreasurespro.model.hibernate.HibernateConstant
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import java.math.BigDecimal
import java.util.*

@DynamicUpdate
@Entity
@Table(
    name = "mob_data",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_mob_data_uuid", columnNames = ["uuid"])
    ]
)
class MobData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null
        protected set

    @NaturalId(mutable = false)
    @Basic(optional = false)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    var uuid: UUID = HibernateConstant.DEFAULT_UUID
        protected set

    @Column(name = "type", nullable = false)
    var mobType: String = HibernateConstant.DEFAULT_STRING
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Int = 1

    @Basic(optional = true)
    @Column(name = "location", nullable = true)
    var location: LocationRetriever? = null

    @Basic(optional = false)
    @Column(name = "bank", nullable = false)
    var bank: BigDecimal = BigDecimal.ZERO

    @Column(name = "level", nullable = false)
    var level: Int = 1

    @Column(name = "is_hologram_enabled", nullable = false)
    var isHologramEnabled: Boolean = true

    constructor()

    constructor(
        mobName: String,
        uuid: UUID,
        location: LocationRetriever?,
    ) {
        this.mobType = mobName
        this.uuid = uuid
        this.location = location
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is org.hibernate.proxy.HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is org.hibernate.proxy.HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as MobData
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        if (this is org.hibernate.proxy.HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
