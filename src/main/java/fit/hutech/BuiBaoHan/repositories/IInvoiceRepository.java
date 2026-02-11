package fit.hutech.BuiBaoHan.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Invoice;
@Repository
public interface IInvoiceRepository extends JpaRepository<Invoice,
Long>{
}

