package com.ketch.android.mock

import mobile.MobileOuterClass

class MockResponses {
    companion object {
        val mockGetConfigurationResponse: MobileOuterClass.GetConfigurationResponse = MobileOuterClass.GetConfigurationResponse.parseFrom(
            java.util.Base64.getDecoder().decode(
                "CgJlbiIICgZheG9uaWMqGQoGZGluZ2h5EgZEaW5naHkaB0FORFJPSUQ6EgoHZGVmYXVsdBIHZGVmYXVsdEIjCgpzd2JfZGluZ2h5EhUKDW1hbmFnZWRDb29raWUSBF9zd2JKIQoKcHJvZHVjdGlvbhoTMzI2MzUxNjAzMzM5MTY4ODQ0MFISCgp2ZGZndmRmZ2RmEMST2f0FWiIKCWF4b25pY19wcBC1mu79BRoPaHR0cHM6Ly9jbm4uY29tYiIKCmF4b25pY190b3MQnZiP/gUaDmF4b25pYy5pby90b3N2akMKCmNtbl9hY2Nlc3MSC0RhdGEgQWNjZXNzGihSaWdodCB0byBiZSBwcm92aWRlZCB3aXRoIGEgY29weSBvZiBkYXRhakoKCGNtbl9wb3J0EhBEYXRhIFBvcnRhYmlsaXR5GixSaWdodCB0byBvYnRhaW4gYW5kIHJlcXVlc3QgdHJhbnNmZXIgb2YgZGF0YWpaCg5jbW5fY29ycmVjdGlvbhIPRGF0YSBDb3JyZWN0aW9uGjdSaWdodCB0byBoYXZlIGluYWNjdXJhdGUgcGVyc29uYWwgaW5mb3JtYXRpb24gY29ycmVjdGVkcgdkZWZhdWx0elwSCWRhdGFzYWxlcxoKRGF0YSBTYWxlcyI3V2Ugd2lsbCBzZWxsIHlvdXIgcGVyc29uYWwgZGF0YSB0byBvdGhlciBpbnN0aXR1dGlvbnMuICoIcmVzZWFyY2hAAXoiEgR0ZXN0GgJkZCICZmYqDmNvbnNlbnRfb3B0b3V0OAFAAXqNARILc2VuZF9lbWFpbHMaD01hcmtldGluZyBFbWFpbCJZV2Ugd2lsbCByZWFjaCBvdXQgdG8geW91IHZpYSBlbWFpbCB0byBwcm92aWRlIHlvdSBvZmZlcnMgb24gZ3JlYXQgZGVhbHMgYW5kIG5ldyBwcm9kdWN0cy4qDmNvbnNlbnRfb3B0b3V0OAFAAXo5EhRmZHNmc2Rmc2Rmc2RnZGZoZ2ZkaBoHZnNkZnNkZiIKZnNkZnNkZnNkZioKZGlzY2xvc3VyZUABej4SDHNvbWV0aGluZ25ldxoMc29tZXRoaW5nbmV3Igxzb21ldGhpbmduZXcqDmNvbnNlbnRfb3B0b3V0OAFAAXouEgZhc2Rhc2QaBmFzZGFzZCIIYXNkYXNhc2QqDmNvbnNlbnRfb3B0b3V0OAFAAXoqEghhc2Rhc2RhcxoFYWRhc2QiA2FzZCoOY29uc2VudF9vcHRvdXQ4AUABeiYSCWFzZGFzZGRjYxoGYXNkYXNkIgNhc2QqCmRpc2Nsb3N1cmVAAXqSARIPcGVyc29uYWxpemF0aW9uGg9QZXJzb25hbGl6YXRpb24iYFdlIHVzZSB0aGlzIGRhdGEgdG8gZ2l2ZSB5b3UgcGVyc29uYWxpemVkIGV4cGVyaWVuY2VzIGFjcm9zcyB2YXJpb3VzIHByb3BlcnRpZXMgeW91IGVuZ2FnZSB3aXRoLioKZGlzY2xvc3VyZUABeicSB2Rhc2Rhc2QaBWFkc2FkIgdhc2RkYXNkKgpkaXNjbG9zdXJlQAF6hwESD3Byb2R1Y3RyZXNlYXJjaBoQUHJvZHVjdCBSZXNlYXJjaCJOV2Ugd2lsbCB1c2UgZGF0YSBjb2xsZWN0ZWQgYWJvdXQgeW91IHRvIHBlcmZvcm0gY3JpdGljYWwgcHJvZHVjdCBlbmhhbmNlbWVudHMuKg5jb25zZW50X29wdG91dDgBQAF6LxIJYXNkZmFzZmRhGgdhc2RmYXNkIgVmYXNmZCoOY29uc2VudF9vcHRvdXQ4AUABeiMSC3Rlc3RWZXJzaW9uGgIxMiICMzQqCmRpc2Nsb3N1cmVAAXogEgJzcxoBZCIDYXNkKg5jb25zZW50X29wdG91dDgBQAF6JhIEbGFuZxoEbGFuZyIEbGFuZyoOY29uc2VudF9vcHRvdXQ4AUABeigSBGFzZGYaCGFzZGZhc2ZkIghhc2Rmc2FkZioKZGlzY2xvc3VyZUABggFBCgpzdXBlcmNhcmdvEjNodHRwczovL3N1cGVyY2FyZ28uZGV2LmIxMHMuaW8vc3VwZXJjYXJnby9jb25maWcvMS+CATgKCndoZWVsaG91c2USKmh0dHBzOi8vd2hlZWxob3VzZS5kZXYuYjEwcy5pby93aGVlbGhvdXNlL4IBKwoJYXN0cm9sYWJlEh5odHRwczovL2Nkbi5iMTBzLmlvL2FzdHJvbGFiZS+CATUKCWdhbmdwbGFuaxIoaHR0cHM6Ly9nYW5ncGxhbmsuZGV2LmIxMHMuaW8vZ2FuZ3BsYW5rL4IBVAoHbGFueWFyZBJJaHR0cHM6Ly90cmFuc29tLmIxMHMuaW8vdHJhbnNvbS9yb3V0ZS9zd2l0Y2hiaXQvbGFueWFyZC9heG9uaWMvbGFueWFyZC5qc4oBEAoMbG9jYWxTdG9yYWdlEAGKAQ0KCW1pZ3JhdGlvbhAB"
            )
        )

        val mockGetConsentResponse: MobileOuterClass.GetConsentResponse =
            MobileOuterClass.GetConsentResponse.newBuilder().addAllConsents(
                arrayListOf(
                    MobileOuterClass.Consent.newBuilder().setPurpose("id1").setLegalBasis("opt_in")
                        .setAllowed(true).build(),
                    MobileOuterClass.Consent.newBuilder().setPurpose("id2").setLegalBasis("opt_out")
                        .setAllowed(false).build()
                )
            )
                .build()
    }
}


