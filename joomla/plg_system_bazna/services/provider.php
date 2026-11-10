<?php
/** Joomla 5/6 dependency injection provider. */
defined('_JEXEC') or die;
use Brendigo\Plugin\System\Bazna\Extension\Bazna;
use Joomla\CMS\Extension\PluginInterface;
use Joomla\CMS\Factory;
use Joomla\CMS\Plugin\PluginHelper;
use Joomla\DI\Container;
use Joomla\DI\ServiceProviderInterface;

return new class implements ServiceProviderInterface {
    public function register(Container $container): void
    {
        $container->set(PluginInterface::class, static function (Container $container): Bazna {
            $plugin = new Bazna(
                $container->get(\Joomla\Event\DispatcherInterface::class),
                (array) PluginHelper::getPlugin('system', 'bazna'),
            );
            $plugin->setApplication(Factory::getApplication());
            return $plugin;
        });
    }
};
