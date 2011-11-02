package nh.scr.example;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {

    BundleComponentActivator bundleComponentActivator = org.apache.felix.scr.impl.Activator.getInstance()
        .getBundleComponentActivator(context.getBundle());
    System.out.println("bundleComponentAcitivator: " + bundleComponentActivator);

    registerHelloService(bundleComponentActivator);
    registerTranslationService(bundleComponentActivator);

  }

  private void registerTranslationService(BundleComponentActivator bundleComponentActivator) {
    ComponentMetadata componentMetadata = new ComponentMetadata(1);
    componentMetadata.setActivate("activate");
    componentMetadata.setName("nh.scr.example.internal.TranslationServiceImpl");
    componentMetadata.setImmediate(true);
    componentMetadata.setImplementationClassName("nh.scr.example.internal.TranslationServiceImpl");
    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.addProvide("nh.scr.example.TranslationService");
    componentMetadata.setService(serviceMetadata);

    System.out.println("Register TranslationService");
    bundleComponentActivator.registerComponent(componentMetadata);

  }

  private void registerHelloService(BundleComponentActivator bundleComponentActivator) {
    ComponentMetadata componentMetadata = new ComponentMetadata(1);
    componentMetadata.setActivate("activate");
    componentMetadata.setName("nh.scr.example.internal.HelloServiceImpl");
    componentMetadata.setImmediate(true);
    componentMetadata.setImplementationClassName("nh.scr.example.internal.HelloServiceImpl");
    ServiceMetadata serviceMetadata = new ServiceMetadata();
    serviceMetadata.addProvide("nh.scr.example.HelloService");
    componentMetadata.setService(serviceMetadata);

    ReferenceMetadata referenceMetadata = new ReferenceMetadata();
    referenceMetadata.setBind("setTranslationService");
    referenceMetadata.setInterface("nh.scr.example.TranslationService");

    componentMetadata.addDependency(referenceMetadata);

    System.out.println("Register HelloService");
    bundleComponentActivator.registerComponent(componentMetadata);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    // TODO Auto-generated method stub

  }

}
