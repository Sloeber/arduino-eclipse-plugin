package cc.arduino.packages.discoverers;

import java.util.Timer;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSTaskStarter;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.tasks.RecordReaper;

public class ArduinoDNSTaskStarter implements DNSTaskStarter.Factory.ClassDelegate {

    @Override
    public DNSTaskStarter newDNSTaskStarter(final JmDNSImpl jmDNSImpl) {
	final DNSTaskStarter.DNSTaskStarterImpl delegate = new DNSTaskStarter.DNSTaskStarterImpl(jmDNSImpl);
	final DNSTaskStarter.DNSTaskStarterImpl.StarterTimer timer = new DNSTaskStarter.DNSTaskStarterImpl.StarterTimer("JmDNS(" //$NON-NLS-1$
		+ jmDNSImpl.getName() + ").Timer", true); //$NON-NLS-1$

	return new DNSTaskStarter() {

	    @Override
	    public void purgeTimer() {
		delegate.purgeTimer();
		timer.purge();
	    }

	    @Override
	    public void purgeStateTimer() {
		delegate.purgeStateTimer();
	    }

	    @Override
	    public void cancelTimer() {
		delegate.cancelTimer();
		timer.cancel();
	    }

	    @Override
	    public void cancelStateTimer() {
		delegate.cancelStateTimer();
	    }

	    @Override
	    public void startProber() {
		delegate.startProber();
	    }

	    @Override
	    public void startAnnouncer() {
		delegate.startAnnouncer();
	    }

	    @Override
	    public void startRenewer() {
		delegate.startRenewer();
	    }

	    @Override
	    public void startCanceler() {
		delegate.startCanceler();
	    }

	    @Override
	    public void startReaper() {
		new RecordReaper(jmDNSImpl) {
		    @Override
		    public void start(Timer _timer) {
			if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
			    _timer.schedule(this, 0, 500);
			}
		    }
		}.start(timer);
	    }

	    @Override
	    public void startServiceInfoResolver(ServiceInfoImpl info) {
		delegate.startServiceInfoResolver(info);
	    }

	    @Override
	    public void startTypeResolver() {
		delegate.startTypeResolver();
	    }

	    @Override
	    public void startServiceResolver(String type) {
		delegate.startServiceResolver(type);
	    }

	    @Override
	    public void startResponder(DNSIncoming in, int port) {
		delegate.startResponder(in, port);
	    }
	};
    }
}
