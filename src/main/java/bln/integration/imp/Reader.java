package bln.integration.imp;

import bln.integration.entity.WorkListHeader;

public interface Reader<T>  {
    void read(WorkListHeader header);
}
