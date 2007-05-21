/*
 * Copyright 2006-2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.module.vendor.fixtures;

import java.util.ArrayList;
import java.util.List;

import org.kuali.kfs.bo.Country;
import org.kuali.kfs.bo.State;
import org.kuali.module.vendor.bo.VendorAddress;
import org.kuali.module.vendor.bo.VendorDetail;
import org.kuali.module.vendor.bo.VendorType;
import org.kuali.module.vendor.fixtures.VendorTestConstants.AddressTypes;

/**
 * This is an enumeration of configurations of 'type codes' and 'address type codes' which should be useful for testing the code
 * validating the implementation of the business rules involving Vendor address types.
 */
public enum VendorRuleAddressTypeFixture {

    WITH_PO_TYPE_AND_PO_ADDR_TYPES(AddressTypes.poType, AddressTypes.poType, true, AddressTypes.poType, false), 
    WITH_DV_TYPE_AND_RM_ADDR_TYPES(AddressTypes.dvType, AddressTypes.rmType, true, AddressTypes.rmType, false), 
    WITH_PO_TYPE_AND_RM_ADDR_TYPES(AddressTypes.poType, AddressTypes.rmType, true, AddressTypes.rmType, false), 
    WITH_PO_TYPE_AND_ONE_PO_AND_ONE_RM_ADDR_TYPES(AddressTypes.poType, AddressTypes.poType, true, AddressTypes.rmType, true), ;

    private String typeCode;
    private String addrTypeCode1;
    private String addrTypeCode2;
    private boolean defaultAddr1;
    private boolean defaultAddr2;

    VendorRuleAddressTypeFixture(String typeCode, String addrTypeCode1, boolean default1, String addrTypeCode2, boolean default2) {
        this.typeCode = typeCode;
        this.addrTypeCode1 = addrTypeCode1;
        this.defaultAddr1 = default1;
        this.addrTypeCode2 = addrTypeCode2;
        this.defaultAddr2 = default2;
    }

    @SuppressWarnings("deprecation")
    public VendorDetail populateVendor(VendorDetail vndr) {
        vndr.getVendorHeader().setVendorTypeCode(this.typeCode);
        VendorType type = new VendorType();
        type.setVendorAddressTypeRequiredCode(this.addrTypeCode1);
        vndr.getVendorHeader().setVendorType(type);
        List<VendorAddress> addrList = new ArrayList();
        VendorAddress addr1 = new VendorAddress();
        VendorAddress addr2 = new VendorAddress();
        addr1.setVendorAddressTypeCode(this.addrTypeCode1);
        addr1.setVendorDefaultAddressIndicator(this.defaultAddr1);
        addr2.setVendorAddressTypeCode(this.addrTypeCode2);
        addr2.setVendorDefaultAddressIndicator(this.defaultAddr2);
        addrList.add(addr1);
        addrList.add(addr2);
        vndr.setVendorAddresses(addrList);
        return vndr;
    }
}
