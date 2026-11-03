package com.ananta.streamcatcher

import android.net.VpnService

/**
 * Reserved entry point for an explicitly user-enabled local VPN monitor.
 * This starter deliberately does not intercept, decrypt, or modify HTTPS traffic.
 * A real packet-forwarding VPN requires a complete, audited tunnel implementation;
 * do not start a TUN interface without forwarding packets or device connectivity will break.
 */
class MonitorVpnService : VpnService()
