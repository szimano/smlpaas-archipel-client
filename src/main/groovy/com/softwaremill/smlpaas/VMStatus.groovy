package com.softwaremill.smlpaas

import groovy.transform.Canonical

@Canonical
class VMStatus {
    String name
    String id
    String status
}
