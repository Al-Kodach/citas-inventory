package com.citacomsys.core.persistence.table.packages

import com.citacomsys.types.ProductModule
import org.jetbrains.exposed.v1.core.Table

 object ProductPackageModules : Table("product_package_modules") {
    val packageId = varchar("package_id", 50).references(ProductPackages.id)
    val module = enumerationByName<ProductModule>("module", 50)

    override val primaryKey = PrimaryKey(packageId, module)
}
